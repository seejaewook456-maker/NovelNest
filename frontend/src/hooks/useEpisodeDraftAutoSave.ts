import { useCallback, useEffect, useRef, useState } from 'react';
import { useBlocker } from 'react-router-dom';
import { saveEpisodeDraft, isEpisodeDraftEmpty, type EpisodeDraftFields } from '../utils/episodeDraftStorage';

export type DraftAutoSaveStatus = 'idle' | 'unsaved' | 'saved';

interface UseEpisodeDraftAutoSaveParams {
  novelId: number | null;
  draft: EpisodeDraftFields;
  // 회차 ID가 없는(신규 작성) 동안 + 복구/새로 작성 여부가 결정된 이후에만 켠다.
  enabled: boolean;
  debounceMs?: number;
}

interface UseEpisodeDraftAutoSaveResult {
  status: DraftAutoSaveStatus;
  hasUnsavedChanges: boolean;
  lastSavedAt: Date | null;
  // 생성 성공 직후 프로그래밍적으로 navigate()할 때 이탈 경고가 한 번 더 뜨는 것을 막기 위한 함수.
  // ref를 직접 갱신하므로 setState의 리렌더를 기다리지 않고 즉시 반영된다.
  suppressLeaveWarning: () => void;
}

const DEFAULT_DEBOUNCE_MS = 2500;

function isSameDraft(a: EpisodeDraftFields | null, b: EpisodeDraftFields): boolean {
  if (!a) return false;
  return a.title === b.title && a.episodeNumber === b.episodeNumber && a.content === b.content;
}

// 신규 회차 작성 화면 전용 자동 저장 훅. 서버 API(useEpisodeAutoSave)와 달리
// 회차 ID가 없으므로 debounce 후 LocalStorage에만 저장하고, 페이지 이탈 경고 정책은 동일하게 맞춘다.
export function useEpisodeDraftAutoSave({
  novelId,
  draft,
  enabled,
  debounceMs = DEFAULT_DEBOUNCE_MS,
}: UseEpisodeDraftAutoSaveParams): UseEpisodeDraftAutoSaveResult {
  const [status, setStatus] = useState<DraftAutoSaveStatus>('idle');
  const [lastSavedAt, setLastSavedAt] = useState<Date | null>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const savedSnapshotRef = useRef<EpisodeDraftFields | null>(null);

  const hasUnsavedChanges = enabled && !isEpisodeDraftEmpty(draft);

  // 이동 차단 predicate가 stale closure를 갖더라도 항상 최신 값을 읽도록 ref로 미러링한다.
  // setState → 리렌더를 기다리지 않고 이벤트 핸들러에서 동기적으로 억제할 수 있어야 하기 때문.
  const hasUnsavedRef = useRef(hasUnsavedChanges);
  const suppressedRef = useRef(false);
  useEffect(() => {
    hasUnsavedRef.current = hasUnsavedChanges;
  });

  const suppressLeaveWarning = useCallback(() => {
    suppressedRef.current = true;
  }, []);

  useEffect(() => {
    if (!enabled || novelId === null) return;

    if (isEpisodeDraftEmpty(draft)) {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
        timerRef.current = null;
      }
      return;
    }
    if (isSameDraft(savedSnapshotRef.current, draft)) return;

    setStatus('unsaved');
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => {
      timerRef.current = null;
      saveEpisodeDraft(novelId, draft);
      savedSnapshotRef.current = draft;
      setLastSavedAt(new Date());
      setStatus('saved');
    }, debounceMs);

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
        timerRef.current = null;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [draft.title, draft.episodeNumber, draft.content, enabled, novelId, debounceMs]);

  // 복구 여부 결정 전이거나 생성 완료 직후처럼 비활성화되면 예약된 저장을 취소한다.
  useEffect(() => {
    if (enabled) return;
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  }, [enabled]);

  // 브라우저 새로고침/탭 닫기 경고 — 서버 자동 저장 훅과 동일한 정책 유지
  useEffect(() => {
    if (!hasUnsavedChanges || suppressedRef.current) return;
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault();
      e.returnValue = '';
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [hasUnsavedChanges]);

  // SPA 내부 이동 차단 — predicate는 ref만 읽는 안정적인 함수라 등록 시점이 아니라
  // navigate() 호출 시점의 최신 상태(특히 suppressLeaveWarning 호출 여부)를 그대로 반영한다.
  const blocker = useBlocker(
    useCallback(
      ({ currentLocation, nextLocation }) =>
        !suppressedRef.current && hasUnsavedRef.current && currentLocation.pathname !== nextLocation.pathname,
      []
    )
  );

  useEffect(() => {
    if (blocker.state !== 'blocked') return;
    const leaveAnyway = window.confirm(
      '작성 중인 내용은 이 브라우저에 임시 저장되었지만, 아직 회차로 생성되지는 않았습니다.\n그래도 페이지를 이동하시겠습니까?'
    );
    if (leaveAnyway) blocker.proceed();
    else blocker.reset();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [blocker.state]);

  return { status, hasUnsavedChanges, lastSavedAt, suppressLeaveWarning };
}

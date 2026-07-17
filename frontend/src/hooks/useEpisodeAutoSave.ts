import { useCallback, useEffect, useRef, useState } from 'react';
import { useBlocker } from 'react-router-dom';
import { updateEpisode } from '../api/episodeApi';
import { isSessionExpired } from '../state/sessionExpired';
import type { Episode } from '../types/episode';

export type AutoSaveStatus = 'idle' | 'unsaved' | 'saving' | 'saved' | 'error';

export interface EpisodeDraft {
  title: string;
  episodeNumber: number;
  content: string;
}

interface UseEpisodeAutoSaveParams {
  episode: Episode | null;
  draft: EpisodeDraft;
  // 편집 화면에서만 동작해야 하므로, 읽기 전용 화면에서는 false로 꺼둔다.
  enabled: boolean;
  debounceMs?: number;
  // 자동/수동 저장 성공 시 페이지의 episode 상태를 최신화하기 위한 콜백.
  onSaved: (updated: Episode) => void;
}

interface UseEpisodeAutoSaveResult {
  status: AutoSaveStatus;
  hasUnsavedChanges: boolean;
  lastSavedAt: Date | null;
  errorMessage: string;
  // 디바운스 타이머를 취소하고 즉시 저장한다. 수동 저장 버튼과 자동 저장이 이 함수를 공유한다.
  saveNow: () => Promise<boolean>;
  // 지정한 스냅샷을 강제로 저장한다. 편집 중 자동 저장으로 서버에 중간 내용이 이미 반영된 뒤
  // "취소"를 누른 경우, 그 중간 내용을 수정 이전 값으로 덮어써 되돌리기 위한 용도.
  revertTo: (snapshot: EpisodeDraft) => Promise<boolean>;
}

const DEFAULT_DEBOUNCE_MS = 2500;

function isSameSnapshot(a: EpisodeDraft | null, b: EpisodeDraft): boolean {
  if (!a) return false;
  // 웹소설 본문은 공백/줄바꿈도 의미 있는 데이터이므로 trim 등 정규화 없이 그대로 비교한다.
  return a.title === b.title && a.episodeNumber === b.episodeNumber && a.content === b.content;
}

// 회차 하나에 대한 저장 진행 상태. render 중에는 절대 읽거나 쓰지 않고
// effect/콜백 안에서만 다룬다(React Compiler의 ref 규칙 준수).
// episodeId가 바뀌면 통째로 초기화되어, 이전 회차의 진행 중이던 저장 요청이
// 뒤늦게 끝나더라도 새 회차 상태를 건드리지 않게 한다.
interface SaveSession {
  episodeId: number | null;
  baseline: EpisodeDraft | null;
  isSaving: boolean;
  pendingResave: boolean;
  timer: ReturnType<typeof setTimeout> | null;
}

export function useEpisodeAutoSave({
  episode,
  draft,
  enabled,
  debounceMs = DEFAULT_DEBOUNCE_MS,
  onSaved,
}: UseEpisodeAutoSaveParams): UseEpisodeAutoSaveResult {
  const [status, setStatus] = useState<AutoSaveStatus>('idle');
  const [lastSavedAt, setLastSavedAt] = useState<Date | null>(null);
  const [errorMessage, setErrorMessage] = useState('');
  // hasUnsavedChanges 계산 등 렌더링에 필요한 값은 반드시 state로 노출한다(ref를 렌더 중 읽지 않기 위함).
  const [baseline, setBaseline] = useState<EpisodeDraft | null>(null);

  const sessionRef = useRef<SaveSession>({
    episodeId: null,
    baseline: null,
    isSaving: false,
    pendingResave: false,
    timer: null,
  });

  // 매 렌더 커밋 이후 최신 draft/onSaved를 ref에 반영한다.
  // performSave는 한 번만 생성되는 안정적인 함수라, 나중에(디바운스 타이머·재저장 체인) 실행될 때도
  // 항상 "그 시점의 최신" 값을 읽도록 하기 위함이다.
  const latestDraftRef = useRef(draft);
  const onSavedRef = useRef(onSaved);
  useEffect(() => {
    latestDraftRef.current = draft;
    onSavedRef.current = onSaved;
  });

  // 회차 전환 또는 최초 로드: 이전 회차의 타이머를 취소하고 세션/기준선을 새로 세팅한다.
  // 최초 조회 데이터가 편집 상태에 주입되는 순간은 저장 대상이 아니므로,
  // 이 시점의 값을 그대로 기준선으로 삼아 "변경 없음" 상태로 시작한다.
  useEffect(() => {
    if (!episode) return;
    const session = sessionRef.current;
    if (session.episodeId === episode.id) return;

    if (session.timer) clearTimeout(session.timer);
    const initialSnapshot: EpisodeDraft = {
      title: episode.title,
      episodeNumber: episode.episodeNumber,
      content: episode.content,
    };
    sessionRef.current = {
      episodeId: episode.id,
      baseline: initialSnapshot,
      isSaving: false,
      pendingResave: false,
      timer: null,
    };
    setBaseline(initialSnapshot);
    setStatus('saved');
    setLastSavedAt(new Date(episode.updatedAt));
    setErrorMessage('');
  }, [episode]);

  const hasUnsavedChanges = enabled && baseline !== null && !isSameSnapshot(baseline, draft);

  // 저장 중 추가 변경이 있었을 때 완료 직후 재귀적으로 다시 저장하기 위한 참조.
  // performSave 안에서 자기 자신의 이름으로 직접 재귀 호출하면 React Compiler가
  // 메모이제이션을 보존하지 못하므로, ref를 한 단계 거쳐 호출한다.
  const performSaveRef = useRef<() => Promise<boolean>>(async () => false);

  // overrideSnapshot을 넘기면 현재 입력값(latestDraftRef) 대신 그 값을 그대로 저장한다.
  // revertTo가 "수정 이전 값으로 강제 되돌리기"에 사용한다.
  const performSave = useCallback(async (overrideSnapshot?: EpisodeDraft): Promise<boolean> => {
    const session = sessionRef.current;
    if (session.episodeId === null || session.baseline === null) return false;
    // 세션이 이미 만료 안내된 상태라면 저장을 계속 시도하지 않는다(무한 재시도 방지).
    if (isSessionExpired()) return false;

    const targetEpisodeId = session.episodeId;
    const snapshot = overrideSnapshot ?? latestDraftRef.current;
    if (isSameSnapshot(session.baseline, snapshot)) {
      setStatus('saved');
      return true;
    }
    if (session.isSaving) {
      // 저장 중 추가 입력이 있었다면, 현재 요청이 끝난 뒤 최신 값으로 다시 저장한다.
      session.pendingResave = true;
      return false;
    }

    session.isSaving = true;
    setStatus('saving');
    setErrorMessage('');
    try {
      const updated = await updateEpisode(targetEpisodeId, snapshot);
      // 요청이 진행되는 동안 다른 회차로 전환됐다면 이 응답 결과는 폐기한다.
      if (sessionRef.current.episodeId !== targetEpisodeId) return true;
      session.baseline = snapshot;
      setBaseline(snapshot);
      onSavedRef.current(updated);
      setLastSavedAt(new Date());
      setStatus('saved');
      return true;
    } catch (err) {
      if (sessionRef.current.episodeId !== targetEpisodeId) return false;
      setStatus('error');
      setErrorMessage(err instanceof Error ? err.message : '저장에 실패했습니다.');
      return false;
    } finally {
      if (sessionRef.current.episodeId === targetEpisodeId) {
        session.isSaving = false;
        if (session.pendingResave) {
          session.pendingResave = false;
          void performSaveRef.current();
        }
      }
    }
  }, []);

  useEffect(() => {
    performSaveRef.current = performSave;
  });

  // 디바운스: 마지막 입력 후 debounceMs 동안 추가 입력이 없으면 저장한다.
  useEffect(() => {
    const session = sessionRef.current;
    if (!enabled || session.episodeId === null || session.baseline === null) return;

    if (isSameSnapshot(session.baseline, draft)) {
      if (session.timer) {
        clearTimeout(session.timer);
        session.timer = null;
      }
      return;
    }

    setStatus('unsaved');
    if (session.timer) clearTimeout(session.timer);
    session.timer = setTimeout(() => {
      session.timer = null;
      void performSave();
    }, debounceMs);

    return () => {
      if (session.timer) {
        clearTimeout(session.timer);
        session.timer = null;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [draft.title, draft.episodeNumber, draft.content, enabled, episode?.id, debounceMs, performSave]);

  // 편집을 벗어나면(취소 등) 예약된 자동 저장은 취소한다.
  useEffect(() => {
    if (enabled) return;
    const session = sessionRef.current;
    if (session.timer) {
      clearTimeout(session.timer);
      session.timer = null;
    }
  }, [enabled]);

  const saveNow = useCallback(async (): Promise<boolean> => {
    const session = sessionRef.current;
    if (session.timer) {
      clearTimeout(session.timer);
      session.timer = null;
    }
    return performSave();
  }, [performSave]);

  const revertTo = useCallback(async (snapshot: EpisodeDraft): Promise<boolean> => {
    const session = sessionRef.current;
    if (session.timer) {
      clearTimeout(session.timer);
      session.timer = null;
    }
    return performSave(snapshot);
  }, [performSave]);

  // 브라우저 새로고침/탭 닫기 경고
  useEffect(() => {
    if (!hasUnsavedChanges) return;
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault();
      e.returnValue = '';
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [hasUnsavedChanges]);

  // SPA 내부 이동(다른 회차 선택, 목록 이동 등) 차단 — 데이터 라우터의 useBlocker 사용.
  const blocker = useBlocker(
    useCallback(
      ({ currentLocation, nextLocation }) =>
        hasUnsavedChanges && currentLocation.pathname !== nextLocation.pathname,
      [hasUnsavedChanges]
    )
  );

  useEffect(() => {
    if (blocker.state !== 'blocked') return;
    let cancelled = false;
    void (async () => {
      // 이동 직전 저장을 먼저 시도하고, 성공하면 경고 없이 그대로 이동한다.
      const ok = await saveNow();
      if (cancelled) return;
      if (ok) {
        blocker.proceed();
        return;
      }
      const leaveAnyway = window.confirm(
        '자동 저장에 실패했습니다.\n저장되지 않은 변경사항이 있습니다. 그래도 페이지를 이동하시겠습니까?'
      );
      if (leaveAnyway) blocker.proceed();
      else blocker.reset();
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [blocker.state]);

  return { status, hasUnsavedChanges, lastSavedAt, errorMessage, saveNow, revertTo };
}

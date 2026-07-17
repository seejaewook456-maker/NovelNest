import { getCurrentUserKey } from './token';

const DRAFT_KEY_PREFIX = 'novelnest:episode-draft';

export interface EpisodeDraftFields {
  title: string;
  episodeNumber: string;
  content: string;
}

interface EpisodeDraftPayload extends EpisodeDraftFields {
  savedAt: string;
}

// 사용자(JWT subject) + 작품 단위로 키를 분리해, 서로 다른 계정/작품의 초안이 섞이지 않게 한다.
function buildDraftKey(novelId: number): string {
  return `${DRAFT_KEY_PREFIX}:${getCurrentUserKey()}:${novelId}`;
}

export function isEpisodeDraftEmpty(draft: EpisodeDraftFields): boolean {
  return draft.title.trim() === '' && draft.content.trim() === '';
}

// 시크릿 모드, 저장 용량 초과 등으로 LocalStorage 접근이 실패해도
// 회차 작성 자체는 계속 진행돼야 하므로 모든 함수는 예외를 삼키고 조용히 무시한다.
export function loadEpisodeDraft(novelId: number): EpisodeDraftFields | null {
  try {
    const raw = window.localStorage.getItem(buildDraftKey(novelId));
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<EpisodeDraftPayload>;
    if (typeof parsed.title !== 'string' || typeof parsed.content !== 'string') return null;
    return {
      title: parsed.title,
      episodeNumber: typeof parsed.episodeNumber === 'string' ? parsed.episodeNumber : '',
      content: parsed.content,
    };
  } catch {
    return null;
  }
}

export function saveEpisodeDraft(novelId: number, draft: EpisodeDraftFields): void {
  try {
    const payload: EpisodeDraftPayload = { ...draft, savedAt: new Date().toISOString() };
    window.localStorage.setItem(buildDraftKey(novelId), JSON.stringify(payload));
  } catch {
    // 저장 실패는 기존 동작(입력 자체)에 영향을 주지 않는다.
  }
}

export function clearEpisodeDraft(novelId: number): void {
  try {
    window.localStorage.removeItem(buildDraftKey(novelId));
  } catch {
    // 삭제 실패해도 무시 — 다음 저장 시 덮어써지므로 사용자 흐름에 영향 없음
  }
}

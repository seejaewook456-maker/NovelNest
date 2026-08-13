import type { Memo } from '../types/memo';

// 즐겨찾기 우선(DESC) → 최근 수정순(DESC) — 백엔드(MemoRepository) 정렬 규칙과 동일하다.
// 메모 관리 페이지(MemoListPage)와 회차 작성/수정 페이지의 메모 메뉴(MemoReferencePanel)가
// 즐겨찾기 토글 직후 로컬에서 낙관적으로 재정렬할 때 서로 다른 기준을 쓰지 않도록,
// 이 함수 하나만 두 곳에서 공유해서 쓴다.
export function sortMemos(list: Memo[]): Memo[] {
  return [...list].sort((a, b) => {
    if (a.isFavorite !== b.isFavorite) return b.isFavorite ? 1 : -1;
    return new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime();
  });
}

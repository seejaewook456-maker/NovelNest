// 페이지네이션 계산 전용 순수 함수 모음 — UI(Pagination 컴포넌트)와 분리해 다른 목록에서도 재사용할 수 있게 한다.

// 페이지 번호 사이를 건너뛸 때 표시하는 생략 기호 자리
export type PageItem = number | 'ellipsis';

// 전체 항목 수 → 전체 페이지 수 (항목이 0개여도 최소 1페이지로 취급)
export function getTotalPages(totalItems: number, pageSize: number): number {
  return Math.max(1, Math.ceil(totalItems / pageSize));
}

// 요청한 페이지를 1 ~ totalPages 범위로 보정 — 삭제로 마지막 페이지가 사라진 경우 마지막 유효 페이지로 맞춘다.
export function clampPage(page: number, totalPages: number): number {
  if (!Number.isFinite(page)) return 1;
  return Math.min(Math.max(1, Math.floor(page)), totalPages);
}

/**
 * 표시할 페이지 번호 목록을 만든다.
 * 항상 첫/마지막 페이지와 현재 페이지 앞뒤 siblings개를 보여주고, 나머지는 'ellipsis'로 접는다.
 * 예) current=6, total=13, siblings=1 → [1, …, 5, 6, 7, …, 13]
 * 페이지 수가 수백 개여도 결과 길이는 최대 (siblings*2 + 5)개로 고정된다.
 */
export function getPageItems(current: number, totalPages: number, siblings = 1): PageItem[] {
  // 접을 필요가 없을 만큼 적으면 전부 표시
  const maxSlots = siblings * 2 + 5;
  if (totalPages <= maxSlots) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }

  const left = Math.max(current - siblings, 2);
  const right = Math.min(current + siblings, totalPages - 1);
  const showLeftEllipsis = left > 3;
  const showRightEllipsis = right < totalPages - 2;

  const items: PageItem[] = [1];

  if (!showLeftEllipsis) {
    // 앞쪽에 있을 때: 1 2 3 4 5 … last (슬롯 수를 일정하게 유지)
    const end = siblings * 2 + 3;
    for (let p = 2; p <= end; p++) items.push(p);
    items.push('ellipsis', totalPages);
    return items;
  }

  if (!showRightEllipsis) {
    // 뒤쪽에 있을 때: 1 … n-4 n-3 n-2 n-1 n
    items.push('ellipsis');
    const start = totalPages - (siblings * 2 + 2);
    for (let p = start; p <= totalPages; p++) items.push(p);
    return items;
  }

  // 가운데에 있을 때: 1 … c-1 c c+1 … last
  items.push('ellipsis');
  for (let p = left; p <= right; p++) items.push(p);
  items.push('ellipsis', totalPages);
  return items;
}

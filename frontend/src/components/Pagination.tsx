import { getPageItems } from '../lib/pagination';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

/**
 * 목록 하단 페이지 이동 바 — [이전] 1 … 4 5 6 … 13 [다음]
 * URL을 바꾸지 않고 부모의 React 상태만 변경한다(onPageChange).
 * 페이지가 1개뿐이면 아무것도 렌더링하지 않는다.
 */
export default function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  const items = getPageItems(currentPage, totalPages);

  return (
    <nav className="pagination" aria-label="페이지 이동">
      <button
        type="button"
        className="pagination-btn pagination-nav"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage <= 1}
        aria-label="이전 페이지"
      >
        ‹<span className="pagination-nav-label"> 이전</span>
      </button>

      {items.map((item, idx) =>
        item === 'ellipsis' ? (
          // 생략 기호는 앞/뒤 두 군데에 나올 수 있어 위치(idx)로 key를 구분한다
          <span key={`ellipsis-${idx}`} className="pagination-ellipsis" aria-hidden="true">
            …
          </span>
        ) : (
          <button
            key={item}
            type="button"
            className={`pagination-btn${item === currentPage ? ' active' : ''}`}
            onClick={() => onPageChange(item)}
            aria-current={item === currentPage ? 'page' : undefined}
            aria-label={`${item}페이지`}
          >
            {item}
          </button>
        ),
      )}

      <button
        type="button"
        className="pagination-btn pagination-nav"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage >= totalPages}
        aria-label="다음 페이지"
      >
        <span className="pagination-nav-label">다음 </span>›
      </button>
    </nav>
  );
}

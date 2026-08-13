import { useEffect, useState } from 'react';
import { getMemos, toggleMemoFavorite } from '../../api/memoApi';
import type { Memo } from '../../types/memo';
import { sortMemos } from '../../utils/memoSort';
import LoadingSpinner from '../LoadingSpinner';
import EmptyState from '../EmptyState';

interface MemoReferencePanelProps {
  novelId: number;
}

// 회차 작성 중 참고용으로 메모 목록을 보여주는 패널.
// 새로운 CRUD를 추가하지 않고 기존 조회/즐겨찾기 API만 재사용하며,
// 전체 편집(작성/수정/삭제)은 기존 메모 관리 페이지를 새 탭으로 열어 그대로 사용한다.
export default function MemoReferencePanel({ novelId }: MemoReferencePanelProps) {
  const [memos, setMemos] = useState<Memo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [expandedId, setExpandedId] = useState<number | null>(null);

  useEffect(() => {
    getMemos(novelId)
      .then((data) => setMemos(sortMemos(data)))
      .catch((err) => setError(err instanceof Error ? err.message : '목록 조회 실패'))
      .finally(() => setLoading(false));
  }, [novelId]);

  const handleToggleFavorite = async (id: number, current: boolean) => {
    try {
      const updated = await toggleMemoFavorite(id, !current);
      setMemos((prev) => sortMemos(prev.map((m) => (m.id === id ? updated : m))));
    } catch {
      // 참고용 패널이므로 실패해도 조용히 무시하고 기존 목록 상태를 유지한다.
    }
  };

  const keyword = search.trim().toLowerCase();
  const filtered = keyword
    ? memos.filter((m) => m.title.toLowerCase().includes(keyword))
    : memos;

  return (
    <div className="workspace-ref-panel">
      <div className="workspace-ref-panel-toolbar">
        <input
          type="text"
          className="workspace-ref-search"
          placeholder="제목 검색"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <a
          className="workspace-ref-manage-link"
          href={`/novels/${novelId}/memos`}
          target="_blank"
          rel="noopener noreferrer"
        >
          전체 관리 ↗
        </a>
      </div>

      {loading ? (
        <LoadingSpinner />
      ) : error ? (
        <p className="error-message">{error}</p>
      ) : filtered.length === 0 ? (
        <EmptyState message={memos.length === 0 ? '작성된 메모가 없습니다.' : '검색 결과가 없습니다.'} />
      ) : (
        filtered.map((m) => {
          const isExpanded = expandedId === m.id;
          // 메모 제목/내용은 사용자가 작성한 콘텐츠이므로 세션 리플레이에서 마스킹한다
          return (
            <div key={m.id} className="item-card workspace-ref-card" data-clarity-mask="true">
              <div
                className="item-card-header workspace-ref-card-header"
                role="button"
                tabIndex={0}
                onClick={() => setExpandedId(isExpanded ? null : m.id)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') setExpandedId(isExpanded ? null : m.id);
                }}
              >
                <h3>{m.title}</h3>
                <button
                  type="button"
                  className={`favorite-btn${m.isFavorite ? ' favorited' : ''}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    void handleToggleFavorite(m.id, m.isFavorite);
                  }}
                  title={m.isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가'}
                >
                  {m.isFavorite ? '★' : '☆'}
                </button>
              </div>
              {isExpanded && <p className="item-field workspace-ref-card-detail">{m.content}</p>}
            </div>
          );
        })
      )}
    </div>
  );
}

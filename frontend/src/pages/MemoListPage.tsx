import { useState, useEffect, type MouseEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getMemos, toggleMemoFavorite } from '../api/memoApi';
import type { Memo } from '../types/memo';
import { sortMemos } from '../utils/memoSort';
import Button from '../components/Button';
import BackLink from '../components/BackLink';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';

export default function MemoListPage() {
  const { novelId } = useParams<{ novelId: string }>();
  const navigate = useNavigate();
  const [memos, setMemos] = useState<Memo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  useEffect(() => {
    if (!novelId) return;
    getMemos(Number(novelId))
      .then((data) => setMemos(sortMemos(data)))
      .catch((err) => setError(err instanceof Error ? err.message : '목록 조회 실패'))
      .finally(() => setLoading(false));
  }, [novelId]);

  const showToast = (message: string, type: 'success' | 'error') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 2000);
  };

  // 즐겨찾기 버튼은 카드 클릭(상세 이동)과 별개의 동작이므로 이벤트 전파를 막고,
  // 서버 응답을 기다린 뒤 sortMemos로 재정렬해 목록 상단/일반 위치로 즉시 이동시킨다.
  const handleToggleFavorite = async (e: MouseEvent, memoId: number, current: boolean) => {
    e.stopPropagation();
    try {
      const updated = await toggleMemoFavorite(memoId, !current);
      setMemos((prev) => sortMemos(prev.map((m) => (m.id === memoId ? updated : m))));
      showToast(!current ? '즐겨찾기에 추가되었습니다.' : '즐겨찾기가 해제되었습니다.', 'success');
    } catch (err) {
      showToast(err instanceof Error ? err.message : '즐겨찾기 변경에 실패했습니다.', 'error');
    }
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <p className="error-message">{error}</p>;

  return (
    <div>
      <BackLink label="← 작품으로" onClick={() => navigate(`/novels/${novelId}`)} />

      <PageHeader
        title="메모 관리"
        action={
          memos.length > 0 ? (
            <Button variant="primary" onClick={() => navigate(`/novels/${novelId}/memos/new`)}>
              + 새 메모 작성
            </Button>
          ) : undefined
        }
      />

      {memos.length === 0 ? (
        <EmptyState
          message="작성된 메모가 없습니다."
          action={
            <Button variant="primary" onClick={() => navigate(`/novels/${novelId}/memos/new`)}>
              첫 번째 메모 작성하기
            </Button>
          }
        />
      ) : (
        <div className="memo-list">
          {memos.map((memo) => (
            // 메모 제목은 사용자가 작성한 콘텐츠이므로 세션 리플레이에서 마스킹한다
            <div
              key={memo.id}
              className="memo-item"
              data-clarity-mask="true"
              onClick={() => navigate(`/memos/${memo.id}`)}
            >
              <div className="memo-item-info">
                <span className="memo-item-title">{memo.title}</span>
                <span className="memo-item-meta">
                  최근 수정: {new Date(memo.updatedAt).toLocaleDateString('ko-KR')}
                </span>
              </div>
              <button
                type="button"
                className={`favorite-btn${memo.isFavorite ? ' favorited' : ''}`}
                onClick={(e) => void handleToggleFavorite(e, memo.id, memo.isFavorite)}
                title={memo.isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가'}
              >
                {memo.isFavorite ? '★' : '☆'}
              </button>
              <span className="memo-arrow">→</span>
            </div>
          ))}
        </div>
      )}

      {toast && <div className={`toast toast-${toast.type}`}>{toast.message}</div>}
    </div>
  );
}

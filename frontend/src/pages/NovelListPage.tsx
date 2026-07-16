import { useState, useEffect } from 'react';
import type { MouseEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyNovels, deleteNovel } from '../api/novelApi';
import type { Novel } from '../types/novel';
import Button from '../components/Button';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import ConfirmDeleteModal from '../components/ConfirmDeleteModal';

export default function NovelListPage() {
  const navigate = useNavigate();
  const [novels, setNovels] = useState<Novel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // 삭제 확인 모달 상태
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  useEffect(() => {
    getMyNovels()
      .then(setNovels)
      .catch((err) => setError(err instanceof Error ? err.message : '목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  // 삭제 버튼 클릭 → 모달만 열기 (API 호출 없음)
  const handleDeleteClick = (e: MouseEvent, novelId: number) => {
    e.stopPropagation();
    setDeleteError('');
    setDeleteTargetId(novelId);
  };

  // 모달에서 삭제 확정 → 기존 삭제 API 호출
  const handleConfirmDelete = async () => {
    if (deleteTargetId === null) return;
    setDeleteLoading(true);
    setDeleteError('');
    try {
      await deleteNovel(deleteTargetId);
      setNovels((prev) => prev.filter((n) => n.id !== deleteTargetId));
      setDeleteTargetId(null);
    } catch (err) {
      setDeleteError(err instanceof Error ? err.message : '삭제에 실패했습니다.');
    } finally {
      setDeleteLoading(false);
    }
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <p className="error-message">{error}</p>;

  return (
    <div>
      <PageHeader
        title="내 작품 목록"
        action={
          novels.length > 0 ? (
            <Button variant="primary" onClick={() => navigate('/novels/new')}>
              + 새 작품
            </Button>
          ) : undefined
        }
      />

      {novels.length === 0 ? (
        <EmptyState
          message="아직 작품이 없습니다."
          action={
            <Button variant="primary" onClick={() => navigate('/novels/new')}>
              첫 번째 작품 만들기
            </Button>
          }
        />
      ) : (
        <div className="novel-grid">
          {novels.map((novel) => (
            <div key={novel.id} className="novel-card" onClick={() => navigate(`/novels/${novel.id}`)}>
              <h3>{novel.title}</h3>
              <span className="genre">{novel.genre}</span>
              {novel.description && <p className="description">{novel.description}</p>}
              <div style={{ marginTop: 14, textAlign: 'right' }}>
                <Button variant="danger" size="sm" onClick={(e) => handleDeleteClick(e, novel.id)}>
                  삭제
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}
      <ConfirmDeleteModal
        isOpen={deleteTargetId !== null}
        title="작품을 삭제하시겠습니까?"
        description="이 작업은 되돌릴 수 없습니다.
작품을 삭제하면 해당 작품의 회차, 등장인물, 세계관 설정, AI 분석 결과가 함께 삭제될 수 있습니다."
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeleteTargetId(null)}
        isLoading={deleteLoading}
        error={deleteError}
      />
    </div>
  );
}

import { useState, useEffect } from 'react';
import type { MouseEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyNovels, deleteNovel } from '../api/novelApi';
import type { Novel } from '../types/novel';
import Button from '../components/Button';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';

export default function NovelListPage() {
  const navigate = useNavigate();
  const [novels, setNovels] = useState<Novel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getMyNovels()
      .then(setNovels)
      .catch((err) => setError(err instanceof Error ? err.message : '목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  const handleDelete = async (e: MouseEvent, novelId: number) => {
    e.stopPropagation();
    if (!confirm('작품을 삭제하시겠습니까?')) return;
    try {
      await deleteNovel(novelId);
      setNovels((prev) => prev.filter((n) => n.id !== novelId));
    } catch (err) {
      alert(err instanceof Error ? err.message : '삭제에 실패했습니다.');
    }
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <p className="error-message">{error}</p>;

  return (
    <div>
      <PageHeader
        title="내 작품 목록"
        action={
          <Button variant="primary" onClick={() => navigate('/novels/new')}>
            + 새 작품
          </Button>
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
                <Button variant="danger" size="sm" onClick={(e) => handleDelete(e, novel.id)}>
                  삭제
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

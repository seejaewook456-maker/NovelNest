import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyNovels, deleteNovel } from '../api/novelApi';
import { Novel } from '../types/novel';

export default function NovelListPage() {
  const navigate = useNavigate();
  const [novels, setNovels] = useState<Novel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchNovels = async () => {
    try {
      const data = await getMyNovels();
      setNovels(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : '목록을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNovels();
  }, []);

  const handleDelete = async (e: React.MouseEvent, novelId: number) => {
    // 카드 클릭 이벤트와 분리
    e.stopPropagation();
    if (!confirm('작품을 삭제하시겠습니까?')) return;

    try {
      await deleteNovel(novelId);
      setNovels((prev) => prev.filter((n) => n.id !== novelId));
    } catch (err) {
      alert(err instanceof Error ? err.message : '삭제에 실패했습니다.');
    }
  };

  if (loading) return <p>불러오는 중...</p>;
  if (error) return <p className="error-message">{error}</p>;

  return (
    <div>
      <div className="page-header">
        <h2>내 작품 목록</h2>
        <button className="btn-primary" style={{ width: 'auto', padding: '10px 20px' }} onClick={() => navigate('/novels/new')}>
          + 새 작품
        </button>
      </div>

      {novels.length === 0 ? (
        <div className="empty-state">
          <p>아직 작품이 없습니다.</p>
          <button className="btn-primary" style={{ width: 'auto', padding: '10px 20px' }} onClick={() => navigate('/novels/new')}>
            첫 번째 작품 만들기
          </button>
        </div>
      ) : (
        <div className="novel-grid">
          {novels.map((novel) => (
            <div key={novel.id} className="novel-card" onClick={() => navigate(`/novels/${novel.id}`)}>
              <h3>{novel.title}</h3>
              <span className="genre">{novel.genre}</span>
              {novel.description && (
                <p className="description">{novel.description}</p>
              )}
              <div style={{ marginTop: '12px', textAlign: 'right' }}>
                <button className="btn-danger" onClick={(e) => handleDelete(e, novel.id)}>
                  삭제
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

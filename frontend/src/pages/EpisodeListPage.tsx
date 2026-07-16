import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getEpisodes } from '../api/episodeApi';
import type { Episode } from '../types/episode';
import Button from '../components/Button';
import BackLink from '../components/BackLink';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';

export default function EpisodeListPage() {
  const { novelId } = useParams<{ novelId: string }>();
  const navigate = useNavigate();
  const [episodes, setEpisodes] = useState<Episode[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!novelId) return;
    getEpisodes(Number(novelId))
      .then(setEpisodes)
      .catch((err) => setError(err instanceof Error ? err.message : '목록 조회 실패'))
      .finally(() => setLoading(false));
  }, [novelId]);

  if (loading) return <LoadingSpinner />;
  if (error) return <p className="error-message">{error}</p>;

  return (
    <div>
      <BackLink label="← 작품으로" onClick={() => navigate(`/novels/${novelId}`)} />

      <PageHeader
        title="회차 목록"
        action={
          episodes.length > 0 ? (
            <Button variant="primary" onClick={() => navigate(`/novels/${novelId}/episodes/new`)}>
              + 새 회차
            </Button>
          ) : undefined
        }
      />

      {episodes.length === 0 ? (
        <EmptyState
          message="아직 작성된 회차가 없습니다."
          action={
            <Button variant="primary" onClick={() => navigate(`/novels/${novelId}/episodes/new`)}>
              첫 번째 회차 작성하기
            </Button>
          }
        />
      ) : (
        <div className="episode-list">
          {episodes.map((ep) => (
            <div key={ep.id} className="episode-item" onClick={() => navigate(`/episodes/${ep.id}`)}>
              <div className="ep-info">
                <span className="ep-num">{ep.episodeNumber}화</span>
                <span className="ep-title">{ep.title}</span>
              </div>
              <span className="ep-arrow">→</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

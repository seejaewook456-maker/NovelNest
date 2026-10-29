import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getEpisodes } from '../api/episodeApi';
import type { Episode } from '../types/episode';
import Button from '../components/Button';
import BackLink from '../components/BackLink';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import Pagination from '../components/Pagination';
import { clampPage, getTotalPages } from '../lib/pagination';

// 한 페이지에 표시할 회차 수
const EPISODES_PER_PAGE = 8;

// 회차 상세/작성/삭제 후 목록으로 돌아와도 보던 페이지가 유지되도록 작품별로 현재 페이지를 기억한다.
// (URL은 바꾸지 않음 — 저장소를 쓸 수 없는 환경이면 1페이지로 시작)
const pageStorageKey = (novelId: string | undefined) => `episodeListPage:${novelId ?? ''}`;

function loadSavedPage(novelId: string | undefined): number {
  try {
    const saved = Number(sessionStorage.getItem(pageStorageKey(novelId)));
    return saved >= 1 ? saved : 1;
  } catch {
    return 1;
  }
}

function savePage(novelId: string | undefined, page: number) {
  try {
    sessionStorage.setItem(pageStorageKey(novelId), String(page));
  } catch {
    // 저장 실패는 무시 — 페이지 이동 자체에는 영향 없음
  }
}

export default function EpisodeListPage() {
  const { novelId } = useParams<{ novelId: string }>();
  const navigate = useNavigate();
  const [episodes, setEpisodes] = useState<Episode[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [currentPage, setCurrentPage] = useState(() => loadSavedPage(novelId));
  const listRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!novelId) return;
    getEpisodes(Number(novelId))
      .then(setEpisodes)
      .catch((err) => setError(err instanceof Error ? err.message : '목록 조회 실패'))
      .finally(() => setLoading(false));
  }, [novelId]);

  if (loading) return <LoadingSpinner />;
  if (error) return <p className="error-message">{error}</p>;

  // 서버가 이미 회차 번호순으로 정렬해 주므로 순서는 그대로 두고 잘라서 보여주기만 한다.
  // 회차 수가 바뀌어(추가/삭제) 현재 페이지가 범위를 벗어나면 렌더링 시점에 마지막 유효 페이지로 보정된다.
  const totalPages = getTotalPages(episodes.length, EPISODES_PER_PAGE);
  const page = clampPage(currentPage, totalPages);
  const pagedEpisodes = episodes.slice((page - 1) * EPISODES_PER_PAGE, page * EPISODES_PER_PAGE);

  const handlePageChange = (next: number) => {
    const target = clampPage(next, totalPages);
    setCurrentPage(target);
    savePage(novelId, target);
    // 목록 윗부분이 화면 위로 스크롤돼 있으면 새 페이지의 첫 회차가 보이도록 목록 시작점으로 올린다
    const listTop = listRef.current?.getBoundingClientRect().top;
    if (listTop !== undefined && listTop < 0) {
      listRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

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
        <>
          <div className="episode-list" ref={listRef}>
            {pagedEpisodes.map((ep) => (
              <div key={ep.id} className="episode-item" onClick={() => navigate(`/episodes/${ep.id}`)}>
                <div className="ep-info">
                  <span className="ep-num">{ep.episodeNumber}화</span>
                  <span className="ep-title">{ep.title}</span>
                </div>
                <span className="ep-arrow">→</span>
              </div>
            ))}
          </div>
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={handlePageChange} />
        </>
      )}
    </div>
  );
}

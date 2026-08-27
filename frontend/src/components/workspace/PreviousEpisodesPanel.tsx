import { useEffect, useMemo, useState } from 'react';
import { getEpisode, getEpisodeBriefs } from '../../api/episodeApi';
import type { Episode, EpisodeBrief } from '../../types/episode';
import LoadingSpinner from '../LoadingSpinner';
import EmptyState from '../EmptyState';
import BackLink from '../BackLink';

interface PreviousEpisodesPanelProps {
  novelId: number;
  // 현재 작성/수정 중인 회차 번호 — 이 값보다 작은 회차만 "이전 회차"로 보여준다.
  // 아직 회차 번호를 입력하지 않은 새 회차 작성 화면에서는 null일 수 있으며,
  // 이 경우 무엇이 "이전"인지 알 수 없으므로 전체 회차를 보여준다.
  currentEpisodeNumber: number | null;
}

// 회차 작성/수정 중 이전 회차 본문을 참고하기 위한 조회 전용(Read-only) 패널.
// 목록 → 상세 전환 구조는 MemoReferencePanel(인라인 아코디언)과 달리, 회차 본문은 길어서
// "목록 화면"과 "상세 화면"을 완전히 분리하고 상세에서는 back 링크로 목록에 돌아가는 방식을 쓴다.
export default function PreviousEpisodesPanel({ novelId, currentEpisodeNumber }: PreviousEpisodesPanelProps) {
  const [episodes, setEpisodes] = useState<EpisodeBrief[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<Episode | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');

  useEffect(() => {
    getEpisodeBriefs(novelId)
      .then((data) => setEpisodes(data))
      .catch((err) => setError(err instanceof Error ? err.message : '목록 조회 실패'))
      .finally(() => setLoading(false));
  }, [novelId]);

  const previousEpisodes = useMemo(() => {
    const filtered =
      currentEpisodeNumber != null
        ? episodes.filter((e) => e.episodeNumber < currentEpisodeNumber)
        : episodes;
    return [...filtered].sort((a, b) => a.episodeNumber - b.episodeNumber);
  }, [episodes, currentEpisodeNumber]);

  const handleSelect = (id: number) => {
    setSelectedId(id);
    // 이전에 보던 회차 내용이 잠깐이라도 남아있지 않도록 즉시 비우고 로딩 상태로 전환한다.
    setDetail(null);
    setDetailError('');
    setDetailLoading(true);
    getEpisode(id)
      .then((data) => setDetail(data))
      .catch((err) => setDetailError(err instanceof Error ? err.message : '회차 조회 실패'))
      .finally(() => setDetailLoading(false));
  };

  const handleBack = () => {
    setSelectedId(null);
    setDetail(null);
    setDetailError('');
  };

  if (selectedId !== null) {
    return (
      <div className="workspace-ref-panel">
        <BackLink label="← 이전 회차 목록" onClick={handleBack} />
        {detailLoading ? (
          <LoadingSpinner />
        ) : detailError ? (
          <p className="error-message">{detailError}</p>
        ) : detail ? (
          <div className="item-card workspace-ref-card" data-clarity-mask="true">
            <div className="item-card-header">
              <h3>{detail.episodeNumber}화 - {detail.title}</h3>
            </div>
            <p className="item-field workspace-ref-card-detail episode-ref-content">{detail.content}</p>
          </div>
        ) : null}
      </div>
    );
  }

  return (
    <div className="workspace-ref-panel">
      {loading ? (
        <LoadingSpinner />
      ) : error ? (
        <p className="error-message">{error}</p>
      ) : previousEpisodes.length === 0 ? (
        <EmptyState message="이전 회차가 없습니다. 첫 회차에서는 이전 회차를 참고할 수 없습니다." />
      ) : (
        previousEpisodes.map((ep) => (
          <div key={ep.id} className="item-card workspace-ref-card" data-clarity-mask="true">
            <div
              className="item-card-header workspace-ref-card-header"
              role="button"
              tabIndex={0}
              onClick={() => handleSelect(ep.id)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleSelect(ep.id);
              }}
            >
              <h3>{ep.episodeNumber}화 - {ep.title}</h3>
            </div>
          </div>
        ))
      )}
    </div>
  );
}

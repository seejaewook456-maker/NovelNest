import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getNovel } from '../api/novelApi';
import type { Novel } from '../types/novel';
import BackLink from '../components/BackLink';
import LoadingSpinner from '../components/LoadingSpinner';
import AiChatPanel from '../components/AiChatPanel';

export default function NovelDetailPage() {
  const { novelId } = useParams<{ novelId: string }>();
  const navigate = useNavigate();

  const [novel, setNovel] = useState<Novel | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!novelId) return;
    const id = Number(novelId);
    getNovel(id)
      .then(setNovel)
      .catch((err) => setError(err instanceof Error ? err.message : '조회 실패'));
  }, [novelId]);

  if (error) return <p className="error-message">{error}</p>;
  if (!novel) return <LoadingSpinner />;

  const id = Number(novelId);

  return (
    <div>
      <BackLink label="← 작품 목록" onClick={() => navigate('/novels')} />

      {/* 작품 제목/장르/소개는 사용자가 작성한 콘텐츠이므로 세션 리플레이에서 마스킹한다 */}
      <div className="novel-info-card" data-clarity-mask="true">
        <h2>{novel.title}</h2>
        <span className="genre-badge">{novel.genre}</span>
        {novel.description && <p className="description">{novel.description}</p>}
      </div>

      {/* 2×2 grid — DOM 순서가 곧 배치 순서(좌→우, 상→하)다:
          1행 [회차 관리][메모 관리], 2행 [등장인물 관리][세계관 관리] */}
      <div className="section-cards">
        <div className="section-card" onClick={() => navigate(`/novels/${id}/episodes`)}>
          <h3>회차 관리</h3>
          <p>회차 목록 조회 및 작성</p>
        </div>
        <div className="section-card" onClick={() => navigate(`/novels/${id}/memos`)}>
          <h3>메모 관리</h3>
          <p>아이디어 및 참고사항 기록</p>
        </div>
        <div className="section-card" onClick={() => navigate(`/novels/${id}/characters`)}>
          <h3>등장인물 관리</h3>
          <p>인물 추가 및 수정</p>
        </div>
        <div className="section-card" onClick={() => navigate(`/novels/${id}/world-settings`)}>
          <h3>세계관 관리</h3>
          <p>설정 추가 및 수정</p>
        </div>
      </div>

      <AiChatPanel novelId={id} />
    </div>
  );
}

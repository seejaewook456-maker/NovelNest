import { useState, useEffect, FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getEpisode, updateEpisode, deleteEpisode } from '../api/episodeApi';
import { getSummary, generateSummary } from '../api/episodeSummaryApi';
import { extractCharacters } from '../api/characterExtractionApi';
import { getEpisodeCharacters } from '../api/episodeCharacterApi';
import type { Episode } from '../types/episode';
import type { EpisodeSummary } from '../types/episodeSummary';
import type { Character } from '../types/character';
import Button from '../components/Button';
import BackLink from '../components/BackLink';
import Card from '../components/Card';
import LoadingSpinner from '../components/LoadingSpinner';

export default function EpisodeDetailPage() {
  const { episodeId } = useParams<{ episodeId: string }>();
  const navigate = useNavigate();
  const [episode, setEpisode] = useState<Episode | null>(null);
  const [error, setError] = useState('');
  const [isEditing, setIsEditing] = useState(false);

  const [editTitle, setEditTitle] = useState('');
  const [editEpisodeNumber, setEditEpisodeNumber] = useState('');
  const [editContent, setEditContent] = useState('');
  const [saving, setSaving] = useState(false);

  const [summary, setSummary] = useState<EpisodeSummary | null>(null);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [summaryError, setSummaryError] = useState('');

  const [extractionLoading, setExtractionLoading] = useState(false);
  const [extractionError, setExtractionError] = useState('');
  const [episodeCharacters, setEpisodeCharacters] = useState<Character[]>([]);

  useEffect(() => {
    if (!episodeId) return;
    const id = Number(episodeId);
    getEpisode(id)
      .then((data) => {
        setEpisode(data);
        setEditTitle(data.title);
        setEditEpisodeNumber(String(data.episodeNumber));
        setEditContent(data.content);
      })
      .catch((err) => setError(err instanceof Error ? err.message : '조회 실패'));

    getSummary(id).then(setSummary);
    getEpisodeCharacters(id).then(setEpisodeCharacters).catch(() => {});
  }, [episodeId]);

  const handleGenerateSummary = async () => {
    if (!episodeId) return;
    setSummaryLoading(true);
    setSummaryError('');
    try {
      const result = await generateSummary(Number(episodeId));
      setSummary(result);
    } catch (err) {
      setSummaryError(err instanceof Error ? err.message : '요약 생성 실패');
    } finally {
      setSummaryLoading(false);
    }
  };

  const handleExtractCharacters = async () => {
    if (!episode) return;
    setExtractionLoading(true);
    setExtractionError('');
    try {
      const result = await extractCharacters(episode.id);
      navigate(`/episodes/${episode.id}/character-review`, {
        state: { candidates: result.candidates, novelId: episode.novelId, episodeId: episode.id, episodeTitle: result.episodeTitle },
      });
    } catch (err) {
      setExtractionError(err instanceof Error ? err.message : '등장인물 추출 실패');
      setExtractionLoading(false);
    }
  };

  const handleUpdate = async (e: FormEvent) => {
    e.preventDefault();
    if (!episode) return;
    setSaving(true);
    try {
      const updated = await updateEpisode(episode.id, {
        title: editTitle,
        episodeNumber: Number(editEpisodeNumber),
        content: editContent,
      });
      setEpisode(updated);
      setIsEditing(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : '수정 실패');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!episode) return;
    if (!window.confirm('이 회차를 삭제하시겠습니까?')) return;
    try {
      await deleteEpisode(episode.id);
      navigate(`/novels/${episode.novelId}/episodes`);
    } catch (err) {
      setError(err instanceof Error ? err.message : '삭제 실패');
    }
  };

  if (error) return <p className="error-message">{error}</p>;
  if (!episode) return <LoadingSpinner />;

  return (
    <div>
      <BackLink label="← 회차 목록" onClick={() => navigate(`/novels/${episode.novelId}/episodes`)} />

      {isEditing ? (
        <div style={{ maxWidth: 680 }}>
          <h2 style={{ marginBottom: 24 }}>회차 수정</h2>
          <Card>
            <form onSubmit={handleUpdate}>
              <div className="form-row">
                <div className="form-group">
                  <label>회차 번호</label>
                  <input
                    type="number"
                    min={1}
                    value={editEpisodeNumber}
                    onChange={(e) => setEditEpisodeNumber(e.target.value)}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>제목</label>
                  <input
                    type="text"
                    value={editTitle}
                    onChange={(e) => setEditTitle(e.target.value)}
                    required
                  />
                </div>
              </div>
              <div className="form-group">
                <label>본문</label>
                <textarea
                  value={editContent}
                  onChange={(e) => setEditContent(e.target.value)}
                  rows={18}
                  required
                />
              </div>
              {error && <p className="error-message">{error}</p>}
              <div className="form-actions">
                <Button type="submit" variant="primary" disabled={saving}>
                  {saving ? '저장 중...' : '저장'}
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    setIsEditing(false);
                    setEditTitle(episode.title);
                    setEditEpisodeNumber(String(episode.episodeNumber));
                    setEditContent(episode.content);
                  }}
                >
                  취소
                </Button>
              </div>
            </form>
          </Card>
        </div>
      ) : (
        <div className="episode-detail">
          <div className="ep-header">
            <div>
              <h2>{episode.title}</h2>
              <p className="ep-num-badge">{episode.episodeNumber}화</p>
            </div>
            <div className="ep-actions">
              <Button variant="secondary" size="sm" onClick={() => setIsEditing(true)}>
                수정
              </Button>
              <Button variant="danger" size="sm" onClick={handleDelete}>
                삭제
              </Button>
            </div>
          </div>

          <div className="episode-content">{episode.content}</div>

          {/* AI 회차 요약 섹션 */}
          <div className="ai-section">
            <div className="ai-section-header">
              <h3>AI 회차 요약</h3>
              <Button variant="primary" size="sm" onClick={handleGenerateSummary} disabled={summaryLoading}>
                {summaryLoading ? '생성 중...' : summary ? '재생성' : 'AI 요약 생성'}
              </Button>
            </div>
            {summaryError && <p className="error-message">{summaryError}</p>}
            {summary ? (
              <div className="summary-box">
                <p className="summary-text">{summary.summary}</p>
                <p className="summary-date">
                  마지막 생성: {new Date(summary.updatedAt).toLocaleString('ko-KR')}
                </p>
              </div>
            ) : (
              !summaryLoading && (
                <p className="summary-empty">아직 요약이 없습니다. AI 요약을 생성해 보세요.</p>
              )
            )}
          </div>

          {/* AI 등장인물 추출 섹션 */}
          <div className="ai-section">
            <div className="ai-section-header">
              <h3>AI 등장인물 추출</h3>
              <Button variant="primary" size="sm" onClick={handleExtractCharacters} disabled={extractionLoading}>
                {extractionLoading ? '분석 중...' : 'AI 등장인물 추출'}
              </Button>
            </div>
            {extractionError && <p className="error-message">{extractionError}</p>}
            {episodeCharacters.length > 0 ? (
              <div className="episode-character-list">
                {episodeCharacters.map((c) => (
                  <div key={c.id} className="episode-character-card">
                    <div className="episode-character-name">{c.name}</div>
                    {c.role && <div className="episode-character-role">{c.role}</div>}
                    <span className="badge-ai-extracted">AI 추출</span>
                  </div>
                ))}
              </div>
            ) : (
              !extractionLoading && (
                <p className="summary-empty">
                  AI가 이 회차의 등장인물을 분석합니다. 추출 후 1명씩 검토해 저장할 수 있습니다.
                </p>
              )
            )}
          </div>
        </div>
      )}
    </div>
  );
}

import { useState, useEffect, useRef, FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getEpisode, updateEpisode, deleteEpisode } from '../api/episodeApi';
import { getSummary, generateSummary } from '../api/episodeSummaryApi';
import { extractCharacters } from '../api/characterExtractionApi';
import { extractWorldSettings } from '../api/worldSettingExtractionApi';
import { getEpisodeCharacters } from '../api/episodeCharacterApi';
import { getEpisodeWorldSettings } from '../api/episodeWorldSettingApi';
import { detectConflicts, getConflictResult } from '../api/conflictDetectionApi';
import type { Episode } from '../types/episode';
import type { EpisodeSummary } from '../types/episodeSummary';
import type { Character } from '../types/character';
import type { WorldSetting } from '../types/worldsetting';
import type { ConflictResult } from '../types/conflictDetection';
import { CATEGORY_LABELS } from '../types/worldsetting';
import { CONFLICT_TYPE_LABELS } from '../types/conflictDetection';
import Button from '../components/Button';
import BackLink from '../components/BackLink';
import Card from '../components/Card';
import LoadingSpinner from '../components/LoadingSpinner';
import ConfirmDeleteModal from '../components/ConfirmDeleteModal';

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

  const [wsExtractionLoading, setWsExtractionLoading] = useState(false);
  const [wsExtractionError, setWsExtractionError] = useState('');
  const [episodeWorldSettings, setEpisodeWorldSettings] = useState<WorldSetting[]>([]);

  const [conflicts, setConflicts] = useState<ConflictResult[]>([]);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [conflictError, setConflictError] = useState('');
  const [hasAnalyzed, setHasAnalyzed] = useState(false);
  const [lastAnalyzedAt, setLastAnalyzedAt] = useState<string | null>(null);

  // 본문 복사 상태
  const [copied, setCopied] = useState(false);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  // 삭제 확인 모달 상태
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  // AI 도구 영역 스크롤 ref
  const aiToolsRef = useRef<HTMLDivElement>(null);
  const scrollToAiTools = () => {
    aiToolsRef.current?.scrollIntoView({ behavior: 'smooth' });
  };
  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

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
    getEpisodeWorldSettings(id).then(setEpisodeWorldSettings).catch(() => {});
    getConflictResult(id)
      .then((result) => {
        if (result) {
          setConflicts(result.conflicts);
          setLastAnalyzedAt(result.analyzedAt);
          setHasAnalyzed(true);
        }
      })
      .catch(() => {});
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

  const handleExtractWorldSettings = async () => {
    if (!episode) return;
    setWsExtractionLoading(true);
    setWsExtractionError('');
    try {
      const result = await extractWorldSettings(episode.id);
      navigate(`/episodes/${episode.id}/world-setting-review`, {
        state: { candidates: result.candidates, novelId: episode.novelId, episodeId: episode.id, episodeTitle: result.episodeTitle },
      });
    } catch (err) {
      setWsExtractionError(err instanceof Error ? err.message : '세계관 추출 실패');
      setWsExtractionLoading(false);
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

  const handleDetectConflicts = async () => {
    if (!episode) return;
    setIsAnalyzing(true);
    setConflictError('');
    try {
      const result = await detectConflicts(episode.id);
      setConflicts(result.conflicts);
      setLastAnalyzedAt(result.analyzedAt);
      setHasAnalyzed(true);
    } catch (err) {
      setConflictError(err instanceof Error ? err.message : '분석 중 오류가 발생했습니다.');
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleCopyContent = async () => {
    if (!episode) return;
    try {
      await navigator.clipboard.writeText(episode.content);
      setCopied(true);
      setToast({ message: '회차 본문이 복사되었습니다.', type: 'success' });
      setTimeout(() => setCopied(false), 2000);
      setTimeout(() => setToast(null), 2000);
    } catch {
      setToast({ message: '본문 복사에 실패했습니다.', type: 'error' });
      setTimeout(() => setToast(null), 2000);
    }
  };

  // 삭제 버튼 클릭 → 모달만 열기 (API 호출 없음)
  const handleDelete = () => {
    setDeleteError('');
    setShowDeleteModal(true);
  };

  // 모달에서 삭제 확정 → 기존 삭제 API 호출
  const handleConfirmDelete = async () => {
    if (!episode) return;
    setDeleteLoading(true);
    setDeleteError('');
    try {
      await deleteEpisode(episode.id);
      navigate(`/novels/${episode.novelId}/episodes`);
    } catch (err) {
      setDeleteError(err instanceof Error ? err.message : '삭제에 실패했습니다.');
      setDeleteLoading(false);
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

          <div className="episode-content-header">
            <span className="episode-content-label">회차 본문</span>
            <div className="episode-content-actions">
              <Button variant="ghost" size="sm" onClick={handleCopyContent}>
                {copied ? '✓ 복사됨' : '📋 본문 복사'}
              </Button>
              <Button variant="ghost" size="sm" onClick={scrollToAiTools}>
                ▼ AI 도구로 이동
              </Button>
            </div>
          </div>

          <div className="episode-content">{episode.content}</div>

          {/* AI 도구 영역 — 스크롤 대상 */}
          <div ref={aiToolsRef}>
            <div className="ai-tools-header">
              <h3 className="ai-tools-title">AI 도구</h3>
              <p className="ai-tools-desc">
                이 회차를 요약하고, 등장인물/세계관을 추출하고, 설정 충돌을 감지할 수 있습니다.
              </p>
            </div>

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

          {/* 세계관 AI 추출 섹션 */}
          <div className="ai-section">
            <div className="ai-section-header">
              <h3>AI 세계관 추출</h3>
              <Button variant="primary" size="sm" onClick={handleExtractWorldSettings} disabled={wsExtractionLoading}>
                {wsExtractionLoading ? '분석 중...' : 'AI 세계관 추출'}
              </Button>
            </div>
            {wsExtractionError && <p className="error-message">{wsExtractionError}</p>}
            {episodeWorldSettings.length > 0 ? (
              <div className="episode-character-list">
                {episodeWorldSettings.map((ws) => (
                  <div key={ws.id} className="episode-character-card">
                    <div className="episode-character-name">{ws.title}</div>
                    <div className="episode-character-role">{CATEGORY_LABELS[ws.category]}</div>
                    <span className="badge-ai-extracted">AI 추출</span>
                  </div>
                ))}
              </div>
            ) : (
              !wsExtractionLoading && (
                <p className="summary-empty">
                  AI가 이 회차의 세계관/설정 정보를 분석합니다. 추출 후 1개씩 검토해 저장할 수 있습니다.
                </p>
              )
            )}
          </div>

          {/* 설정 충돌 감지 섹션 */}
          <div className="ai-section">
            <div className="ai-section-header">
              <h3>설정 충돌 감지</h3>
              <Button variant="primary" size="sm" onClick={handleDetectConflicts} disabled={isAnalyzing}>
                {isAnalyzing ? 'AI가 충돌을 분석 중...' : hasAnalyzed ? '재분석' : '설정 충돌 감지'}
              </Button>
            </div>
            {conflictError && <p className="error-message">{conflictError}</p>}
            {hasAnalyzed && !isAnalyzing && (
              <>
                {lastAnalyzedAt && (
                  <p className="summary-date">
                    마지막 분석: {new Date(lastAnalyzedAt).toLocaleString('ko-KR')}
                  </p>
                )}
                {/* 요약 바 */}
                <ConflictSummaryBar conflicts={conflicts} />
                {/* 충돌 카드 목록 */}
                {conflicts.length > 0 ? (
                  <div>
                    {conflicts.map((conflict, i) => (
                      <ConflictCard key={i} conflict={conflict} />
                    ))}
                  </div>
                ) : (
                  <p className="summary-empty">
                    현재 회차에서 뚜렷한 설정 충돌은 발견되지 않았습니다.
                  </p>
                )}
              </>
            )}
            {!hasAnalyzed && !isAnalyzing && (
              <p className="summary-empty">
                등장인물, 세계관, 이전 회차 요약과 현재 본문을 비교해 충돌 가능성을 분석합니다.
              </p>
            )}
          </div>

          {/* 최상단 이동 버튼 */}
          <div className="ai-tools-footer">
            <Button variant="ghost" size="sm" onClick={scrollToTop}>
              ▲ 최상단으로 이동
            </Button>
          </div>
          </div>
        </div>
      )}

      <ConfirmDeleteModal
        isOpen={showDeleteModal}
        title="회차를 삭제하시겠습니까?"
        description="이 작업은 되돌릴 수 없습니다.
회차를 삭제하면 해당 회차의 본문, 요약, AI 분석 결과가 함께 삭제될 수 있습니다."
        onConfirm={handleConfirmDelete}
        onCancel={() => setShowDeleteModal(false)}
        isLoading={deleteLoading}
        error={deleteError}
      />

      {toast && (
        <div className={`toast toast-${toast.type}`}>
          {toast.message}
        </div>
      )}
    </div>
  );
}

// severity 값을 CSS 클래스 suffix로 변환
function severityClass(severity: string): string {
  return severity.toLowerCase();
}

// 요약 바 — HIGH/MEDIUM/LOW 건수 표시
function ConflictSummaryBar({ conflicts }: { conflicts: ConflictResult[] }) {
  const highCount   = conflicts.filter((c) => c.severity === 'HIGH').length;
  const mediumCount = conflicts.filter((c) => c.severity === 'MEDIUM').length;
  const lowCount    = conflicts.filter((c) => c.severity === 'LOW').length;

  if (conflicts.length === 0) return null;

  return (
    <div className="conflict-summary-bar">
      <span className="conflict-summary-text">
        총 {conflicts.length}건의 충돌 가능성을 발견했습니다.
      </span>
      <div className="conflict-summary-counts">
        {highCount > 0 && (
          <span className="conflict-count-badge conflict-count-badge-high">HIGH {highCount}</span>
        )}
        {mediumCount > 0 && (
          <span className="conflict-count-badge conflict-count-badge-medium">MEDIUM {mediumCount}</span>
        )}
        {lowCount > 0 && (
          <span className="conflict-count-badge conflict-count-badge-low">LOW {lowCount}</span>
        )}
      </div>
    </div>
  );
}

// 개별 충돌 카드
function ConflictCard({ conflict }: { conflict: ConflictResult }) {
  const sev = severityClass(conflict.severity);
  const typeLabel = CONFLICT_TYPE_LABELS[conflict.type as keyof typeof CONFLICT_TYPE_LABELS]
    ?? conflict.type;

  return (
    <div className={`conflict-card conflict-card-${sev}`}>
      <div className="conflict-card-header">
        <span className={`severity-badge severity-badge-${sev}`}>{conflict.severity}</span>
        <span className="conflict-type-label">{typeLabel}</span>
      </div>
      <p className="conflict-card-title">{conflict.title}</p>
      <div className="conflict-info-section">
        <span className="conflict-info-label">기존 설정</span>
        <p className="conflict-info-text">{conflict.existingInfo}</p>
      </div>
      <div className="conflict-info-section">
        <span className="conflict-info-label">현재 회차 내용</span>
        <p className="conflict-info-text">{conflict.currentEpisodeInfo}</p>
      </div>
      <div className="conflict-info-section conflict-info-section-description">
        <span className="conflict-info-label">AI 설명</span>
        <p className="conflict-info-text">{conflict.description}</p>
      </div>
      <div className="conflict-info-section conflict-info-section-suggestion">
        <span className="conflict-info-label">AI 제안</span>
        <p className="conflict-info-text">{conflict.suggestion}</p>
      </div>
    </div>
  );
}

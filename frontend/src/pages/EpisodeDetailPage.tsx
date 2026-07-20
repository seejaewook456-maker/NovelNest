import { useState, useEffect, useRef, useCallback, type FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getEpisode, deleteEpisode } from '../api/episodeApi';
import { useEpisodeAutoSave } from '../hooks/useEpisodeAutoSave';
import type { EpisodeDraft } from '../hooks/useEpisodeAutoSave';
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
import RevertConfirmModal from '../components/RevertConfirmModal';
import EpisodeWorkspace from '../components/workspace/EpisodeWorkspace';
import EditorHeader from '../components/workspace/EditorHeader';
import WritingAssistToolbar from '../components/WritingAssistToolbar';

export default function EpisodeDetailPage() {
  const { episodeId } = useParams<{ episodeId: string }>();
  const navigate = useNavigate();
  const [episode, setEpisode] = useState<Episode | null>(null);
  const [error, setError] = useState('');
  const [isEditing, setIsEditing] = useState(false);

  const [editTitle, setEditTitle] = useState('');
  const [editEpisodeNumber, setEditEpisodeNumber] = useState('');
  const [editContent, setEditContent] = useState('');
  const editContentRef = useRef<HTMLTextAreaElement>(null);

  // 자동 저장 — episodeNumber는 입력 중 잠깐 빈 값이 되어도(예: 지우고 다시 입력)
  // 유효하지 않은 값으로 저장을 시도하지 않도록 원래 값으로 폴백한다.
  const handleAutoSaved = useCallback((updated: Episode) => {
    setEpisode(updated);
  }, []);
  const autoSave = useEpisodeAutoSave({
    episode,
    draft: {
      title: editTitle,
      episodeNumber: Number(editEpisodeNumber) || episode?.episodeNumber || 1,
      content: editContent,
    },
    enabled: isEditing,
    onSaved: handleAutoSaved,
  });

  // 수정 시작 시점의 값 — 편집 도중 자동 저장이 서버에 중간 내용을 반영해도
  // "취소" 시 되돌아갈 진짜 원본을 알기 위해 별도로 보관한다(episode는 자동 저장마다 갱신되므로 못 씀).
  const preEditSnapshotRef = useRef<EpisodeDraft | null>(null);
  const [cancelLoading, setCancelLoading] = useState(false);
  const [showCancelConfirm, setShowCancelConfirm] = useState(false);

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
  const toastTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  // 연속으로 토스트를 띄울 때 이전 타이머가 방금 띄운 토스트를 조기에 지우지 않도록 타이머를 교체한다.
  const showToast = useCallback((message: string, type: 'success' | 'error') => {
    if (toastTimerRef.current) clearTimeout(toastTimerRef.current);
    setToast({ message, type });
    toastTimerRef.current = setTimeout(() => {
      toastTimerRef.current = null;
      setToast(null);
    }, 2000);
  }, []);

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

  // 수동 저장 — 자동 저장과 동일한 saveNow()를 공유해 저장 로직이 중복되지 않는다.
  // 디바운스 타이머가 있었다면 saveNow 내부에서 즉시 저장한다.
  const handleUpdate = async (e: FormEvent) => {
    e.preventDefault();
    const ok = await autoSave.saveNow();
    if (ok) setIsEditing(false);
  };

  // 수정 시작 — "취소" 시 되돌아갈 기준값을 이 시점의 episode로 고정해둔다.
  const handleStartEdit = () => {
    if (!episode) return;
    preEditSnapshotRef.current = {
      title: episode.title,
      episodeNumber: episode.episodeNumber,
      content: episode.content,
    };
    setIsEditing(true);
  };

  // 취소 버튼 클릭 — 곧바로 되돌리지 않고, 되돌릴 내용이 실제로 있을 때만 확인 모달을 띄운다.
  // (아무것도 바뀌지 않았다면 물어볼 필요 없이 바로 편집을 종료한다.)
  const handleCancelClick = () => {
    const preEdit = preEditSnapshotRef.current;
    if (!episode || !preEdit) {
      setIsEditing(false);
      return;
    }

    const hasChanges =
      episode.title !== preEdit.title ||
      episode.episodeNumber !== preEdit.episodeNumber ||
      episode.content !== preEdit.content ||
      editTitle !== preEdit.title ||
      Number(editEpisodeNumber) !== preEdit.episodeNumber ||
      editContent !== preEdit.content;

    if (!hasChanges) {
      setIsEditing(false);
      return;
    }

    setShowCancelConfirm(true);
  };

  // 확인 모달에서 "아니오" — 아무것도 되돌리지 않고 수정 화면을 그대로 유지한다.
  const handleCancelConfirmNo = () => {
    setShowCancelConfirm(false);
  };

  // 확인 모달에서 "예" — 편집 중 자동 저장이 서버에 중간 내용을 이미 반영했다면, 그 내용을
  // 수정 시작 시점 값으로 덮어써 되돌린 뒤에만 편집을 종료한다(자동 저장분이 그대로 남지 않도록).
  const handleCancelConfirmYes = async () => {
    const preEdit = preEditSnapshotRef.current;
    if (!episode || !preEdit) {
      setShowCancelConfirm(false);
      setIsEditing(false);
      return;
    }

    const persistedDuringEdit =
      episode.title !== preEdit.title ||
      episode.episodeNumber !== preEdit.episodeNumber ||
      episode.content !== preEdit.content;

    if (!persistedDuringEdit) {
      // 자동 저장이 아직 한 번도 반영되지 않았다면 서버에 되돌릴 것이 없으므로 화면만 되돌린다.
      setShowCancelConfirm(false);
      setIsEditing(false);
      setEditTitle(episode.title);
      setEditEpisodeNumber(String(episode.episodeNumber));
      setEditContent(episode.content);
      return;
    }

    showToast('수정 이전 버전으로 돌아갑니다.', 'success');
    setCancelLoading(true);
    const ok = await autoSave.revertTo(preEdit);
    setCancelLoading(false);
    setShowCancelConfirm(false);

    if (ok) {
      setIsEditing(false);
      setEditTitle(preEdit.title);
      setEditEpisodeNumber(String(preEdit.episodeNumber));
      setEditContent(preEdit.content);
    } else {
      showToast('이전 버전으로 되돌리지 못했습니다. 다시 시도해주세요.', 'error');
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
      showToast('회차 본문이 복사되었습니다.', 'success');
      setTimeout(() => setCopied(false), 2000);
    } catch {
      showToast('본문 복사에 실패했습니다.', 'error');
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
    <>
      {/* 메뉴(등장인물/세계관/AI 채팅)는 회차 작성·수정 화면에서만 제공한다.
          읽기 전용 상세 화면(else 분기)에는 EpisodeWorkspace를 아예 사용하지 않는다. */}
      {isEditing ? (
        // 뒤로가기/제목/저장 상태를 EpisodeWorkspace의 children(=입력 박스와 같은 flex 컬럼) 안에
        // 함께 렌더링해, 패널 열림/닫힘·화면 크기 변경과 무관하게 입력 박스와 항상 같은 위치를 유지한다.
        <EpisodeWorkspace novelId={episode.novelId} fixedContentWidth={680}>
          <EditorHeader
            backLabel="← 회차 목록"
            onBack={() => navigate(`/novels/${episode.novelId}/episodes`)}
            title="회차 수정"
            statusBadge={<AutoSaveStatusBadge autoSave={autoSave} />}
          />
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
                <WritingAssistToolbar
                  content={editContent}
                  onChange={setEditContent}
                  textareaRef={editContentRef}
                />
                <textarea
                  ref={editContentRef}
                  value={editContent}
                  onChange={(e) => setEditContent(e.target.value)}
                  rows={18}
                  required
                />
              </div>
              {autoSave.status === 'error' && (
                <p className="error-message">
                  {autoSave.errorMessage || '저장에 실패했습니다.'}{' '}
                  <button type="button" className="link-button" onClick={() => void autoSave.saveNow()}>
                    다시 시도
                  </button>
                </p>
              )}
              <div className="form-actions">
                <Button
                  type="submit"
                  variant="primary"
                  disabled={autoSave.status === 'saving' || cancelLoading}
                >
                  {autoSave.status === 'saving' ? '저장 중...' : '저장'}
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  disabled={autoSave.status === 'saving' || cancelLoading}
                  onClick={handleCancelClick}
                >
                  취소
                </Button>
              </div>
            </form>
          </Card>
        </EpisodeWorkspace>
      ) : (
        <>
          <BackLink label="← 회차 목록" onClick={() => navigate(`/novels/${episode.novelId}/episodes`)} />
        <div className="episode-detail">
          <div className="ep-header">
            <div>
              <h2>{episode.title}</h2>
              <p className="ep-num-badge">{episode.episodeNumber}화</p>
            </div>
            <div className="ep-actions">
              <Button variant="secondary" size="sm" onClick={handleStartEdit}>
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
        </>
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

      <RevertConfirmModal
        isOpen={showCancelConfirm}
        onConfirm={() => void handleCancelConfirmYes()}
        onCancel={handleCancelConfirmNo}
        loading={cancelLoading}
      />

      {toast && (
        <div className={`toast toast-${toast.type}`}>
          {toast.message}
        </div>
      )}
    </>
  );
}

// 자동 저장 상태 배지 — "변경사항 있음 / 저장 중.../ 저장됨 / 저장 실패"
function AutoSaveStatusBadge({ autoSave }: { autoSave: ReturnType<typeof useEpisodeAutoSave> }) {
  const { status, lastSavedAt } = autoSave;

  if (status === 'saving') {
    return (
      <span className="autosave-status autosave-status-saving">
        <span className="autosave-spinner" /> 저장 중...
      </span>
    );
  }
  if (status === 'error') {
    return <span className="autosave-status autosave-status-error">⚠ 저장 실패</span>;
  }
  if (status === 'unsaved') {
    return <span className="autosave-status autosave-status-unsaved">저장되지 않은 변경사항이 있습니다.</span>;
  }
  if (status === 'saved') {
    const timeText = lastSavedAt
      ? lastSavedAt.toLocaleTimeString('ko-KR', { hour: 'numeric', minute: '2-digit' })
      : '';
    return <span className="autosave-status autosave-status-saved">{timeText && `${timeText} `}저장됨</span>;
  }
  return null;
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

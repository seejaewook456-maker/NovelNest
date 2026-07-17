import { useState, useRef, useMemo, type FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { createEpisode } from '../api/episodeApi';
import { useEpisodeDraftAutoSave, type DraftAutoSaveStatus } from '../hooks/useEpisodeDraftAutoSave';
import { loadEpisodeDraft, saveEpisodeDraft, clearEpisodeDraft } from '../utils/episodeDraftStorage';
import type { EpisodeDraftFields } from '../utils/episodeDraftStorage';
import Button from '../components/Button';
import BackLink from '../components/BackLink';
import Card from '../components/Card';
import WritingAssistToolbar from '../components/WritingAssistToolbar';
import DraftRecoveryModal from '../components/DraftRecoveryModal';

export default function EpisodeCreatePage() {
  const { novelId } = useParams<{ novelId: string }>();
  const navigate = useNavigate();
  const numericNovelId = novelId ? Number(novelId) : null;

  const [title, setTitle] = useState('');
  const [episodeNumber, setEpisodeNumber] = useState('');
  const [content, setContent] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const contentRef = useRef<HTMLTextAreaElement>(null);

  // 초안 복구 여부가 결정되기 전까지는 폼을 열지 않는다(초안이 빈 입력으로 덮어써지는 것을 방지).
  // 마운트 시점에 한 번만 LocalStorage를 동기적으로 확인한다(effect에서 setState하지 않기 위해 lazy init 사용).
  const [pendingDraft, setPendingDraft] = useState<EpisodeDraftFields | null>(() =>
    numericNovelId !== null ? loadEpisodeDraft(numericNovelId) : null
  );
  // 생성 성공 직후에는 자동 저장/이탈 경고를 즉시 끄기 위한 플래그.
  const [justCreated, setJustCreated] = useState(false);

  const draft = useMemo<EpisodeDraftFields>(
    () => ({ title, episodeNumber, content }),
    [title, episodeNumber, content]
  );

  const draftAutoSave = useEpisodeDraftAutoSave({
    novelId: numericNovelId,
    draft,
    enabled: pendingDraft === null && !justCreated,
  });

  const handleRecoverDraft = () => {
    if (!pendingDraft) return;
    setTitle(pendingDraft.title);
    setEpisodeNumber(pendingDraft.episodeNumber);
    setContent(pendingDraft.content);
    setPendingDraft(null);
  };

  const handleDiscardDraft = () => {
    if (numericNovelId !== null) clearEpisodeDraft(numericNovelId);
    setPendingDraft(null);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (numericNovelId === null) return;
    setError('');
    setLoading(true);
    // 디바운스 대기 중 최신 입력이 아직 LocalStorage에 반영되지 않았을 수 있으므로,
    // 생성 요청 직전 값을 즉시 반영해 실패 시에도 최신 내용을 잃지 않게 한다.
    saveEpisodeDraft(numericNovelId, draft);
    try {
      await createEpisode(numericNovelId, {
        title,
        episodeNumber: Number(episodeNumber),
        content,
      });
      clearEpisodeDraft(numericNovelId);
      setJustCreated(true);
      // 생성 직후의 navigate()는 이동 차단 대상이 아니므로, 리렌더를 기다리지 않고 즉시 경고를 억제한다.
      draftAutoSave.suppressLeaveWarning();
      navigate(`/novels/${novelId}/episodes`);
    } catch (err) {
      setError(err instanceof Error ? err.message : '회차 생성 실패');
    } finally {
      setLoading(false);
    }
  };

  if (pendingDraft) {
    return (
      <div style={{ maxWidth: 680 }}>
        <BackLink label="← 회차 목록" onClick={() => navigate(`/novels/${novelId}/episodes`)} />
        <h2 style={{ marginBottom: 24 }}>새 회차 작성</h2>
        <DraftRecoveryModal isOpen onRecover={handleRecoverDraft} onDiscard={handleDiscardDraft} />
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 680 }}>
      <BackLink label="← 회차 목록" onClick={() => navigate(`/novels/${novelId}/episodes`)} />
      <div className="episode-content-header" style={{ marginTop: 0 }}>
        <h2 style={{ margin: 0 }}>새 회차 작성</h2>
        <DraftAutoSaveStatusBadge status={draftAutoSave.status} lastSavedAt={draftAutoSave.lastSavedAt} />
      </div>
      <Card>
        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="form-group">
              <label>회차 번호</label>
              <input
                type="number"
                min={1}
                value={episodeNumber}
                onChange={(e) => setEpisodeNumber(e.target.value)}
                placeholder="예) 1"
                required
              />
            </div>
            <div className="form-group">
              <label>제목</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="회차 제목"
                required
              />
            </div>
          </div>
          <div className="form-group">
            <label>본문</label>
            <WritingAssistToolbar
              content={content}
              onChange={setContent}
              textareaRef={contentRef}
            />
            <textarea
              ref={contentRef}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="회차 내용을 입력하세요"
              rows={18}
              required
            />
          </div>
          {error && <p className="error-message">{error}</p>}
          <div className="form-actions">
            <Button type="submit" variant="primary" disabled={loading}>
              {loading ? '저장 중...' : '회차 저장'}
            </Button>
            <Button type="button" variant="secondary" onClick={() => navigate(`/novels/${novelId}/episodes`)}>
              취소
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}

// 신규 회차의 LocalStorage 임시 저장 상태 배지 — 서버 자동 저장 배지와 동일한 스타일을 재사용한다.
function DraftAutoSaveStatusBadge({
  status,
  lastSavedAt,
}: {
  status: DraftAutoSaveStatus;
  lastSavedAt: Date | null;
}) {
  if (status === 'unsaved') {
    return <span className="autosave-status autosave-status-unsaved">저장되지 않은 변경사항이 있습니다.</span>;
  }
  if (status === 'saved') {
    const timeText = lastSavedAt
      ? lastSavedAt.toLocaleTimeString('ko-KR', { hour: 'numeric', minute: '2-digit' })
      : '';
    return <span className="autosave-status autosave-status-saved">{timeText && `${timeText} `}임시 저장됨</span>;
  }
  return null;
}

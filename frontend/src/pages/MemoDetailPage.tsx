import { useState, useEffect, type FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getMemo, updateMemo, deleteMemo } from '../api/memoApi';
import { trackEvent } from '../lib/analytics';
import { ANALYTICS_EVENTS } from '../constants/analyticsEvents';
import type { Memo } from '../types/memo';
import Button from '../components/Button';
import BackLink from '../components/BackLink';
import LoadingSpinner from '../components/LoadingSpinner';
import ConfirmDeleteModal from '../components/ConfirmDeleteModal';

export default function MemoDetailPage() {
  const { memoId } = useParams<{ memoId: string }>();
  const navigate = useNavigate();
  const [memo, setMemo] = useState<Memo | null>(null);
  const [error, setError] = useState('');
  const [isEditing, setIsEditing] = useState(false);

  const [editTitle, setEditTitle] = useState('');
  const [editContent, setEditContent] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');

  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const showToast = (message: string, type: 'success' | 'error') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 2000);
  };

  useEffect(() => {
    if (!memoId) return;
    getMemo(Number(memoId))
      .then((data) => {
        setMemo(data);
        setEditTitle(data.title);
        setEditContent(data.content);
      })
      .catch((err) => setError(err instanceof Error ? err.message : '조회 실패'));
  }, [memoId]);

  const handleStartEdit = () => {
    if (!memo) return;
    setEditTitle(memo.title);
    setEditContent(memo.content);
    setSaveError('');
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    if (!memo) return;
    setEditTitle(memo.title);
    setEditContent(memo.content);
    setIsEditing(false);
  };

  const handleUpdate = async (e: FormEvent) => {
    e.preventDefault();
    if (!memo) return;
    setSaving(true);
    setSaveError('');
    try {
      const updated = await updateMemo(memo.id, { title: editTitle, content: editContent });
      setMemo(updated);
      trackEvent(ANALYTICS_EVENTS.MEMO_UPDATE);
      setIsEditing(false);
      showToast('메모가 저장되었습니다.', 'success');
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : '저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = () => {
    setDeleteError('');
    setShowDeleteModal(true);
  };

  const handleConfirmDelete = async () => {
    if (!memo) return;
    setDeleteLoading(true);
    setDeleteError('');
    try {
      await deleteMemo(memo.id);
      navigate(`/novels/${memo.novelId}/memos`);
    } catch (err) {
      setDeleteError(err instanceof Error ? err.message : '삭제에 실패했습니다.');
      setDeleteLoading(false);
    }
  };

  if (error) return <p className="error-message">{error}</p>;
  if (!memo) return <LoadingSpinner />;

  return (
    <>
      <div style={{ maxWidth: 680 }}>
        <BackLink label="← 메모 목록" onClick={() => navigate(`/novels/${memo.novelId}/memos`)} />

        <div className="memo-detail">
          {isEditing ? (
            // 메모 제목/내용은 사용자가 작성한 콘텐츠이므로 세션 리플레이에서 마스킹한다
            <form onSubmit={handleUpdate} data-clarity-mask="true">
              <div className="form-group">
                <label>제목</label>
                <input
                  type="text"
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                  required
                />
              </div>
              <div className="form-group">
                <label>내용</label>
                <textarea
                  value={editContent}
                  onChange={(e) => setEditContent(e.target.value)}
                  rows={14}
                  required
                />
              </div>
              {saveError && <p className="error-message">{saveError}</p>}
              <div className="form-actions">
                <Button type="submit" variant="primary" disabled={saving}>
                  {saving ? '저장 중...' : '저장'}
                </Button>
                <Button type="button" variant="secondary" onClick={handleCancelEdit} disabled={saving}>
                  취소
                </Button>
              </div>
            </form>
          ) : (
            <>
              <div className="memo-header" data-clarity-mask="true">
                <h2>{memo.title}</h2>
                <div className="memo-actions">
                  <Button variant="secondary" size="sm" onClick={handleStartEdit}>
                    수정
                  </Button>
                  <Button variant="danger" size="sm" onClick={handleDelete}>
                    삭제
                  </Button>
                </div>
              </div>
              <p className="memo-updated-at">
                최근 수정: {new Date(memo.updatedAt).toLocaleString('ko-KR')}
              </p>
              <div className="memo-content" data-clarity-mask="true">{memo.content}</div>
            </>
          )}
        </div>
      </div>

      <ConfirmDeleteModal
        isOpen={showDeleteModal}
        title="메모를 삭제하시겠습니까?"
        description="이 작업은 되돌릴 수 없습니다. 메모 내용이 완전히 삭제됩니다."
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
    </>
  );
}

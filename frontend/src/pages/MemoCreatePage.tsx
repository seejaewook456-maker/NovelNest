import { useState, type FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { createMemo } from '../api/memoApi';
import { trackEvent } from '../lib/analytics';
import { ANALYTICS_EVENTS } from '../constants/analyticsEvents';
import Button from '../components/Button';
import BackLink from '../components/BackLink';
import PageHeader from '../components/PageHeader';
import Card from '../components/Card';

export default function MemoCreatePage() {
  const { novelId } = useParams<{ novelId: string }>();
  const navigate = useNavigate();
  const numericNovelId = novelId ? Number(novelId) : null;

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (numericNovelId === null) return;
    setError('');
    setLoading(true);
    try {
      const created = await createMemo(numericNovelId, { title, content });
      trackEvent(ANALYTICS_EVENTS.MEMO_CREATE);
      navigate(`/memos/${created.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : '메모 생성 실패');
    } finally {
      setLoading(false);
    }
  };

  if (numericNovelId === null) return null;

  return (
    <div style={{ maxWidth: 680 }}>
      <BackLink label="← 메모 목록" onClick={() => navigate(`/novels/${novelId}/memos`)} />
      <PageHeader title="새 메모 작성" />
      <Card>
        {/* 메모 제목/내용은 사용자가 작성한 콘텐츠이므로 세션 리플레이에서 마스킹한다 */}
        <form onSubmit={handleSubmit} data-clarity-mask="true">
          <div className="form-group">
            <label>제목</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="메모 제목"
              required
            />
          </div>
          <div className="form-group">
            <label>내용</label>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="자유롭게 메모를 작성하세요"
              rows={14}
              required
            />
          </div>
          {error && <p className="error-message">{error}</p>}
          <div className="form-actions">
            <Button type="submit" variant="primary" disabled={loading}>
              {loading ? '저장 중...' : '메모 저장'}
            </Button>
            <Button type="button" variant="secondary" onClick={() => navigate(`/novels/${novelId}/memos`)}>
              취소
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}

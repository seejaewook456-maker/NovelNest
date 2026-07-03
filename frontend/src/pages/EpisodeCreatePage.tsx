import { useState, useRef, type FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { createEpisode } from '../api/episodeApi';
import Button from '../components/Button';
import BackLink from '../components/BackLink';
import Card from '../components/Card';
import WritingAssistToolbar from '../components/WritingAssistToolbar';

export default function EpisodeCreatePage() {
  const { novelId } = useParams<{ novelId: string }>();
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [episodeNumber, setEpisodeNumber] = useState('');
  const [content, setContent] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const contentRef = useRef<HTMLTextAreaElement>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await createEpisode(Number(novelId), {
        title,
        episodeNumber: Number(episodeNumber),
        content,
      });
      navigate(`/novels/${novelId}/episodes`);
    } catch (err) {
      setError(err instanceof Error ? err.message : '회차 생성 실패');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 680 }}>
      <BackLink label="← 회차 목록" onClick={() => navigate(`/novels/${novelId}/episodes`)} />
      <h2 style={{ marginBottom: 24 }}>새 회차 작성</h2>
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

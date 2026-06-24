import { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { createNovel } from '../api/novelApi';
import Button from '../components/Button';
import BackLink from '../components/BackLink';
import Card from '../components/Card';

export default function NovelCreatePage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [genre, setGenre] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await createNovel({ title, genre, description: description || undefined });
      navigate('/novels');
    } catch (err) {
      setError(err instanceof Error ? err.message : '작품 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 560 }}>
      <BackLink label="← 작품 목록" onClick={() => navigate('/novels')} />
      <h2 style={{ marginBottom: 24 }}>새 작품 만들기</h2>
      <Card>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>제목</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="작품 제목을 입력하세요"
              required
            />
          </div>
          <div className="form-group">
            <label>장르</label>
            <input
              type="text"
              value={genre}
              onChange={(e) => setGenre(e.target.value)}
              placeholder="예) 판타지, 로맨스, SF"
              required
            />
          </div>
          <div className="form-group">
            <label>작품 소개 (선택)</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="간단한 작품 소개를 입력하세요"
              rows={3}
            />
          </div>
          {error && <p className="error-message">{error}</p>}
          <div className="form-actions">
            <Button type="submit" variant="primary" disabled={loading}>
              {loading ? '생성 중...' : '작품 만들기'}
            </Button>
            <Button type="button" variant="secondary" onClick={() => navigate('/novels')}>
              취소
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}

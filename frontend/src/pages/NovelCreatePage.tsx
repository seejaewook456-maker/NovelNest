import { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { createNovel } from '../api/novelApi';

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
    <div className="form-container">
      <h1>새 작품 만들기</h1>
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
          <input
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="간단한 작품 소개를 입력하세요"
          />
        </div>
        {error && <p className="error-message">{error}</p>}
        <button type="submit" className="btn-primary" disabled={loading}>
          {loading ? '생성 중...' : '작품 만들기'}
        </button>
      </form>
      <p className="form-link">
        <span style={{ cursor: 'pointer', color: '#4f6ef7', fontWeight: 600 }} onClick={() => navigate('/novels')}>
          ← 목록으로 돌아가기
        </span>
      </p>
    </div>
  );
}

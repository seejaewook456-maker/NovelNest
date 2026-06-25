import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { saveToken } from '../utils/token';

export default function OAuth2CallbackPage() {
  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');

    if (token) {
      saveToken(token);
      navigate('/novels', { replace: true });
    } else {
      navigate('/login', { replace: true });
    }
  }, [navigate]);

  return (
    <div className="auth-wrapper">
      <div className="auth-card" style={{ textAlign: 'center' }}>
        <p style={{ color: 'var(--color-text-secondary)', fontSize: '15px' }}>
          로그인 처리 중...
        </p>
      </div>
    </div>
  );
}

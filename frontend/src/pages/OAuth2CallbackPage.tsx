import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { saveTokens } from '../utils/token';
import { consumeOAuthProvider } from '../utils/oauthProvider';
import { trackEvent } from '../lib/analytics';
import { ANALYTICS_EVENTS } from '../constants/analyticsEvents';

export default function OAuth2CallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const token = searchParams.get('token');
    const refreshToken = searchParams.get('refreshToken');

    if (token && refreshToken) {
      saveTokens(token, refreshToken);
      // 백엔드 리다이렉트는 provider 정보를 넘겨주지 않으므로, 로그인 버튼 클릭 시
      // 저장해둔 값으로 method를 채운다(신규 가입/기존 로그인 구분은 백엔드 신호가 없어 불가능).
      const provider = consumeOAuthProvider();
      trackEvent(ANALYTICS_EVENTS.LOGIN, provider ? { method: provider } : undefined);
      navigate('/novels', { replace: true });
    } else {
      navigate('/login', { replace: true });
    }
  }, [navigate, searchParams]);

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

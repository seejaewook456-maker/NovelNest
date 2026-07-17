import { useState, useEffect, useRef, type FormEvent } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { login, logout } from '../api/authApi';
import { BACKEND_BASE_URL } from '../api/config';
import { saveTokens, clearTokens } from '../utils/token';
import Button from '../components/Button';

// 브랜드 연필 아이콘 (favicon.svg와 동일한 컨셉의 인라인 SVG)
function PencilIcon() {
  return (
    <svg viewBox="0 0 64 64" width="48" height="48" aria-hidden="true">
      <g transform="translate(24,50) rotate(26) scale(0.82)">
        <path
          d="M-6.5,-48 A6.5,6.5 0 0 1 6.5,-48 L6.5,-11 L0,0 L-6.5,-11 Z"
          fill="#70492E"
        />
        <rect x="-6.5" y="-41" width="13" height="3" fill="#FFFFFF" />
        <path d="M-3.5,-15 L2.5,-9 L-3.5,-3 L-0.5,-9 Z" fill="#FFFFFF" />
      </g>
    </svg>
  );
}

// Google 로고 SVG (CDN 없이 인라인으로 삽입, XSS 위험 없음)
function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
      <path
        fill="#4285F4"
        d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.717v2.258h2.908c1.702-1.567 2.684-3.874 2.684-6.615z"
      />
      <path
        fill="#34A853"
        d="M9 18c2.43 0 4.467-.806 5.956-2.184l-2.908-2.258c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332C2.438 15.983 5.482 18 9 18z"
      />
      <path
        fill="#FBBC05"
        d="M3.964 10.707c-.18-.54-.282-1.117-.282-1.707s.102-1.167.282-1.707V4.961H.957C.347 6.175 0 7.55 0 9s.348 2.825.957 4.039l3.007-2.332z"
      />
      <path
        fill="#EA4335"
        d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0 5.482 0 2.438 2.017.957 4.961L3.964 7.293C4.672 5.166 6.656 3.58 9 3.58z"
      />
    </svg>
  );
}

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Google OAuth 실패 시 백엔드가 ?error=... 파라미터를 붙여 리다이렉트함
  // (세션 만료 안내는 SessionExpiredModal이 로그인 페이지 이동 전에 먼저 보여준다)
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const oauthError = params.get('error');
    if (oauthError) {
      setError(decodeURIComponent(oauthError));
    }
  }, [location.search]);

  // 실제 로그아웃 마무리(서버 Refresh Token 무효화 + 로컬 토큰 삭제).
  // MainLayout의 로그아웃 버튼은 이 페이지로의 이동이 실제로 성공했을 때만 여기 도달하므로,
  // 편집 중 페이지의 "저장되지 않은 변경사항" 이동 차단이 사용자의 취소를 정상적으로 존중할 수 있다.
  const logoutFinalizedRef = useRef(false);
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    if (params.get('logout') !== '1' || logoutFinalizedRef.current) return;
    logoutFinalizedRef.current = true;
    void (async () => {
      try {
        await logout();
      } catch {
        // 네트워크 오류 등으로 서버 로그아웃에 실패해도 사용자는 로그아웃되어야 하므로 무시
      } finally {
        clearTokens();
      }
    })();
  }, [location.search]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const { accessToken, refreshToken } = await login({ email, password });
      saveTokens(accessToken, refreshToken);
      navigate('/novels');
    } catch (err) {
      setError(err instanceof Error ? err.message : '로그인에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card login-card">
        <div className="auth-brand-header">
          <div className="auth-brand-icon">
            <PencilIcon />
          </div>
          <h1 className="auth-brand-name">노벨네스트</h1>
          <p className="auth-brand-tagline">AI 기반 웹소설 집필 도구</p>
          <p className="auth-brand-desc">
            AI와 함께 등장인물, 세계관, 회차를
            <br />
            체계적으로 관리하세요.
          </p>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>이메일</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="user@example.com"
              required
            />
          </div>
          <div className="form-group">
            <label>비밀번호</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="비밀번호를 입력하세요"
              required
            />
          </div>
          <p className="form-link" style={{ textAlign: 'right', margin: '-4px 0 12px' }}>
            <Link to="/forgot-password">비밀번호를 잊으셨나요?</Link>
          </p>
          {error && <p className="error-message">{error}</p>}
          <Button
            type="submit"
            variant="primary"
            fullWidth
            disabled={loading}
            className="login-submit-btn"
            style={{ marginTop: 8 }}
          >
            {loading ? '로그인 중...' : '로그인'}
          </Button>
        </form>

        {/* 구분선 */}
        <div className="auth-divider">또는</div>

        {/* Google 로그인: <a> 태그로 백엔드 OAuth2 진입점에 직접 이동 */}
        <a
          href={`${BACKEND_BASE_URL}/oauth2/authorization/google`}
          className="btn-google login-google-btn"
        >
          <GoogleIcon />
          Google로 로그인
        </a>

        {/* 카카오 로그인 */}
        <a
          href={`${BACKEND_BASE_URL}/oauth2/authorization/kakao`}
          className="btn-kakao login-kakao-btn"
        >
          카카오로 로그인
        </a>

        <p className="form-link">
          계정이 없으신가요? <Link to="/signup">회원가입</Link>
        </p>
      </div>
    </div>
  );
}

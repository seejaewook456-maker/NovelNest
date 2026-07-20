import { useState, useEffect, type ChangeEvent, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { signup, sendEmailVerificationCode, verifyEmailCode } from '../api/authApi';
import { BACKEND_BASE_URL } from '../api/config';
import Button from '../components/Button';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const RESEND_INTERVAL_SECONDS = 60;

// 카카오 로고 SVG (말풍선 아이콘, CDN 없이 인라인으로 삽입)
function KakaoIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
      <path
        fill="#191919"
        d="M9 1.5C4.44 1.5 0.75 4.44 0.75 8.07c0 2.34 1.56 4.4 3.9 5.58-.17.62-.62 2.27-.71 2.62-.11.44.16.43.34.31.14-.09 2.24-1.52 3.15-2.14.5.07 1.02.11 1.57.11 4.56 0 8.25-2.94 8.25-6.57S13.56 1.5 9 1.5z"
      />
    </svg>
  );
}

// Google 로고 SVG (인라인, CDN 의존 없음)
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

export default function SignupPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [nickname, setNickname] = useState('');
  const [loading, setLoading] = useState(false);

  // 이메일 인증 관련 상태
  const [verificationCode, setVerificationCode] = useState('');
  const [isCodeSent, setIsCodeSent] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isVerifyingCode, setIsVerifyingCode] = useState(false);
  const [resendSeconds, setResendSeconds] = useState(0);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // 60초 재전송 카운트다운
  useEffect(() => {
    if (resendSeconds <= 0) return;
    const timer = setInterval(() => {
      setResendSeconds((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [resendSeconds]);

  const handleEmailChange = (e: ChangeEvent<HTMLInputElement>) => {
    setEmail(e.target.value);
    // 인증번호를 이미 발송했던 이메일을 수정하면 인증 상태를 초기화
    if (isCodeSent) {
      setIsCodeSent(false);
      setIsEmailVerified(false);
      setVerificationCode('');
      setResendSeconds(0);
      setSuccessMessage('');
      setErrorMessage('');
    }
  };

  const handleCodeChange = (e: ChangeEvent<HTMLInputElement>) => {
    setVerificationCode(e.target.value.replace(/\D/g, '').slice(0, 6));
  };

  const handleSendCode = async () => {
    setErrorMessage('');
    setSuccessMessage('');
    setIsSendingCode(true);
    try {
      await sendEmailVerificationCode({ email });
      setIsCodeSent(true);
      setResendSeconds(RESEND_INTERVAL_SECONDS);
      setSuccessMessage('인증번호가 이메일로 발송되었습니다.');
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : '인증번호 발송에 실패했습니다.');
    } finally {
      setIsSendingCode(false);
    }
  };

  const handleVerifyCode = async () => {
    setErrorMessage('');
    setSuccessMessage('');
    setIsVerifyingCode(true);
    try {
      await verifyEmailCode({ email, code: verificationCode });
      setIsEmailVerified(true);
      setSuccessMessage('이메일 인증이 완료되었습니다.');
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : '인증번호 확인에 실패했습니다.');
    } finally {
      setIsVerifyingCode(false);
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!isEmailVerified) {
      setErrorMessage('이메일 인증을 먼저 완료해주세요.');
      return;
    }
    if (password !== passwordConfirm) {
      setErrorMessage('비밀번호가 일치하지 않습니다.');
      return;
    }
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      await signup({ email, password, passwordConfirm, nickname });
      // 완료 메시지를 보여준 뒤 짧은 안내 후 로그인 화면으로 이동 (버튼은 계속 비활성 상태로 유지해 중복 제출 방지)
      setSuccessMessage('회원가입이 완료되었습니다. 로그인 화면으로 이동합니다.');
      setTimeout(() => navigate('/login'), 1500);
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : '회원가입에 실패했습니다.');
      setLoading(false);
    }
  };

  const sendCodeLabel = isEmailVerified
    ? '인증 완료'
    : isSendingCode
      ? '전송 중...'
      : resendSeconds > 0
        ? `${resendSeconds}초 후 재전송`
        : isCodeSent
          ? '인증번호 재전송'
          : '인증번호 전송';

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <div className="auth-header">
          <p className="auth-brand">노벨네스트</p>
          <h2 className="auth-title">회원가입</h2>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>이메일</label>
            <div className="input-with-button">
              <input
                type="email"
                value={email}
                onChange={handleEmailChange}
                placeholder="user@example.com"
                disabled={isEmailVerified}
                required
              />
              <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={handleSendCode}
                disabled={isSendingCode || isEmailVerified || resendSeconds > 0 || !EMAIL_REGEX.test(email)}
              >
                {sendCodeLabel}
              </Button>
            </div>
          </div>

          {isCodeSent && !isEmailVerified && (
            <div className="form-group">
              <label>인증번호</label>
              <div className="input-with-button">
                <input
                  type="text"
                  inputMode="numeric"
                  value={verificationCode}
                  onChange={handleCodeChange}
                  placeholder="6자리 숫자"
                  maxLength={6}
                />
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={handleVerifyCode}
                  disabled={isVerifyingCode || verificationCode.length !== 6}
                >
                  {isVerifyingCode ? '확인 중...' : '인증번호 확인'}
                </Button>
              </div>
            </div>
          )}

          {errorMessage && <p className="error-message">{errorMessage}</p>}
          {!errorMessage && successMessage && <p className="success-message">{successMessage}</p>}

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
          <div className="form-group">
            <label>비밀번호 재입력</label>
            <input
              type="password"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              placeholder="비밀번호를 다시 입력해주세요."
              required
            />
            {passwordConfirm && (
              <p
                className={
                  password === passwordConfirm
                    ? 'password-match-hint match'
                    : 'password-match-hint mismatch'
                }
              >
                {password === passwordConfirm
                  ? '비밀번호가 일치합니다.'
                  : '비밀번호가 일치하지 않습니다.'}
              </p>
            )}
          </div>
          <div className="form-group">
            <label>닉네임</label>
            <input
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="닉네임을 입력하세요"
              required
            />
          </div>
          <Button
            type="submit"
            variant="primary"
            fullWidth
            disabled={loading || !isEmailVerified || password !== passwordConfirm}
            style={{ marginTop: 8 }}
          >
            {loading ? '처리 중...' : '회원가입'}
          </Button>
        </form>

        {/* 구분선 */}
        <div className="auth-divider">또는</div>

        {/* Google로 바로 회원가입: 계정이 없으면 자동 가입, 있으면 로그인 */}
        <a
          href={`${BACKEND_BASE_URL}/oauth2/authorization/google`}
          className="btn-google"
        >
          <GoogleIcon />
          Google로 회원가입
        </a>

        {/* 카카오로 바로 회원가입: 계정이 없으면 자동 가입, 있으면 로그인 (LoginPage와 동일한 진입점/스타일) */}
        <a
          href={`${BACKEND_BASE_URL}/oauth2/authorization/kakao`}
          className="btn-kakao"
        >
          <KakaoIcon />
          카카오로 회원가입
        </a>

        <p className="form-link">
          이미 계정이 있으신가요? <Link to="/login">로그인</Link>
        </p>
      </div>
    </div>
  );
}

import { useState, useEffect, type ChangeEvent, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  requestPasswordResetCode,
  verifyPasswordResetCode,
  confirmPasswordReset,
} from '../api/authApi';
import Button from '../components/Button';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const RESEND_INTERVAL_SECONDS = 60;

type Step = 'email' | 'code' | 'password' | 'done';

// resetToken/이메일/인증번호는 전부 컴포넌트 메모리(state)에만 보관하고
// localStorage/sessionStorage/URL에는 절대 남기지 않는다 — 새로고침 시 자연스럽게 1단계로 초기화된다.
export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<Step>('email');

  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [resetToken, setResetToken] = useState('');

  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showPasswordConfirm, setShowPasswordConfirm] = useState(false);

  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isVerifyingCode, setIsVerifyingCode] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [resendSeconds, setResendSeconds] = useState(0);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // 60초 재전송 카운트다운 (SignupPage와 동일한 패턴)
  useEffect(() => {
    if (resendSeconds <= 0) return;
    const timer = setInterval(() => {
      setResendSeconds((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [resendSeconds]);

  // 완료 단계 진입 후 짧은 안내 뒤 로그인 화면으로 자동 이동
  useEffect(() => {
    if (step !== 'done') return;
    const timer = setTimeout(() => navigate('/login'), 2000);
    return () => clearTimeout(timer);
  }, [step, navigate]);

  const resetToEmailStep = () => {
    setStep('email');
    setCode('');
    setResetToken('');
    setResendSeconds(0);
    setErrorMessage('');
    setSuccessMessage('');
  };

  const handleCodeChange = (e: ChangeEvent<HTMLInputElement>) => {
    setCode(e.target.value.replace(/\D/g, '').slice(0, 6));
  };

  const handleSendCode = async () => {
    setErrorMessage('');
    setSuccessMessage('');
    setIsSendingCode(true);
    try {
      await requestPasswordResetCode({ email });
      setStep('code');
      setResendSeconds(RESEND_INTERVAL_SECONDS);
      setSuccessMessage('입력하신 이메일로 가입된 계정이 있는 경우 인증번호가 발송됩니다.');
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : '인증번호 발송에 실패했습니다.');
    } finally {
      setIsSendingCode(false);
    }
  };

  const handleSendCodeSubmit = (e: FormEvent) => {
    e.preventDefault();
    handleSendCode();
  };

  const handleVerifyCode = async (e: FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setIsVerifyingCode(true);
    try {
      const { resetToken: token } = await verifyPasswordResetCode({ email, code });
      setResetToken(token);
      setStep('password');
      setSuccessMessage('');
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : '인증번호 확인에 실패했습니다.');
    } finally {
      setIsVerifyingCode(false);
    }
  };

  const handleConfirmPassword = async (e: FormEvent) => {
    e.preventDefault();
    if (newPassword !== newPasswordConfirm) {
      setErrorMessage('비밀번호가 일치하지 않습니다.');
      return;
    }
    setErrorMessage('');
    setIsSubmitting(true);
    try {
      await confirmPasswordReset({ resetToken, newPassword, newPasswordConfirm });
      // 재사용 방지를 위해 완료 즉시 재설정 관련 상태를 모두 비운다
      setResetToken('');
      setCode('');
      setStep('done');
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : '비밀번호 변경에 실패했습니다.');
      setIsSubmitting(false);
    }
  };

  const sendCodeLabel = isSendingCode
    ? '전송 중...'
    : resendSeconds > 0
      ? `${resendSeconds}초 후 재전송`
      : '인증번호 재전송';

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <div className="auth-header">
          <p className="auth-brand">노벨네스트</p>
          <h2 className="auth-title">비밀번호 찾기</h2>
        </div>

        {step === 'email' && (
          <form onSubmit={handleSendCodeSubmit}>
            <p style={{ color: 'var(--color-text-primary)', marginBottom: 16 }}>
              가입할 때 사용한 이메일을 입력해주세요.
            </p>
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
            {errorMessage && <p className="error-message">{errorMessage}</p>}
            <Button
              type="submit"
              variant="primary"
              fullWidth
              disabled={isSendingCode || !EMAIL_REGEX.test(email)}
              style={{ marginTop: 8 }}
            >
              {isSendingCode ? '발송 중...' : '인증번호 발송'}
            </Button>
            <p className="form-link">
              <Link to="/login">로그인 화면으로 돌아가기</Link>
            </p>
          </form>
        )}

        {step === 'code' && (
          <form onSubmit={handleVerifyCode}>
            <p style={{ color: 'var(--color-text-primary)', marginBottom: 16 }}>
              <strong>{email}</strong>(으)로 발송된 인증번호를 입력해주세요.
              <br />
              인증번호는 5분간 유효합니다.
            </p>
            <div className="form-group">
              <label>인증번호</label>
              <div className="input-with-button">
                <input
                  type="text"
                  inputMode="numeric"
                  value={code}
                  onChange={handleCodeChange}
                  placeholder="6자리 숫자"
                  maxLength={6}
                  autoFocus
                />
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={handleSendCode}
                  disabled={isSendingCode || resendSeconds > 0}
                >
                  {sendCodeLabel}
                </Button>
              </div>
            </div>
            {errorMessage && <p className="error-message">{errorMessage}</p>}
            {!errorMessage && successMessage && <p className="success-message">{successMessage}</p>}
            <Button
              type="submit"
              variant="primary"
              fullWidth
              disabled={isVerifyingCode || code.length !== 6}
              style={{ marginTop: 8 }}
            >
              {isVerifyingCode ? '확인 중...' : '인증번호 확인'}
            </Button>
            <p className="form-link">
              <span onClick={resetToEmailStep}>이메일 다시 입력하기</span>
            </p>
          </form>
        )}

        {step === 'password' && (
          <form onSubmit={handleConfirmPassword}>
            <div className="form-group">
              <label>새 비밀번호</label>
              <div className="input-with-button">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="새 비밀번호를 입력하세요"
                  required
                  autoFocus
                />
                <Button type="button" variant="secondary" size="sm" onClick={() => setShowPassword((v) => !v)}>
                  {showPassword ? '숨기기' : '표시'}
                </Button>
              </div>
            </div>
            <div className="form-group">
              <label>새 비밀번호 재입력</label>
              <div className="input-with-button">
                <input
                  type={showPasswordConfirm ? 'text' : 'password'}
                  value={newPasswordConfirm}
                  onChange={(e) => setNewPasswordConfirm(e.target.value)}
                  placeholder="새 비밀번호를 다시 입력해주세요"
                  required
                />
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={() => setShowPasswordConfirm((v) => !v)}
                >
                  {showPasswordConfirm ? '숨기기' : '표시'}
                </Button>
              </div>
              {newPasswordConfirm && (
                <p
                  className={
                    newPassword === newPasswordConfirm
                      ? 'password-match-hint match'
                      : 'password-match-hint mismatch'
                  }
                >
                  {newPassword === newPasswordConfirm ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.'}
                </p>
              )}
            </div>
            {errorMessage && <p className="error-message">{errorMessage}</p>}
            <Button
              type="submit"
              variant="primary"
              fullWidth
              disabled={isSubmitting || !newPassword || newPassword !== newPasswordConfirm}
              style={{ marginTop: 8 }}
            >
              {isSubmitting ? '변경 중...' : '비밀번호 변경'}
            </Button>
          </form>
        )}

        {step === 'done' && (
          <div>
            <p className="success-message">비밀번호가 변경되었습니다.</p>
            <p style={{ color: 'var(--color-text-primary)', marginBottom: 16 }}>
              잠시 후 로그인 화면으로 이동합니다.
            </p>
            <Button type="button" variant="primary" fullWidth onClick={() => navigate('/login')}>
              로그인 화면으로 이동
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}

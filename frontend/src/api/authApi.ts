import type {
  LoginRequest,
  SignupRequest,
  LoginData,
  EmailSendCodeRequest,
  EmailVerifyCodeRequest,
  PasswordResetCodeRequest,
  PasswordResetVerifyRequest,
  PasswordResetVerifyData,
  PasswordResetConfirmRequest,
} from '../types/auth';
import { API_BASE_URL } from './config';
import { fetchWithAuth } from './fetchWithAuth';

// 백엔드 공통 응답 구조
interface ApiResponse<T = undefined> {
  message: string;
  data?: T;
}

export const login = async (body: LoginRequest): Promise<LoginData> => {
  const res = await fetch(`${API_BASE_URL}/users/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  const json: ApiResponse<LoginData> = await res.json();

  if (!res.ok) {
    throw new Error(json.message || '로그인에 실패했습니다.');
  }

  // 백엔드 응답: { message: "로그인 성공", data: { accessToken: "...", refreshToken: "..." } }
  return json.data!;
};

// 로그아웃 — 서버에 저장된 Refresh Token을 무효화한다 (로컬 토큰 삭제는 호출부에서 처리)
export const logout = async (): Promise<void> => {
  await fetchWithAuth('/auth/logout', { method: 'POST' });
};

export const signup = async (body: SignupRequest): Promise<void> => {
  const res = await fetch(`${API_BASE_URL}/users/signup`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  const json: ApiResponse = await res.json();

  if (!res.ok) {
    throw new Error(json.message || '회원가입에 실패했습니다.');
  }
};

// 이메일 인증번호 발송 — 일반 이메일 회원가입 전 이메일 소유 확인용 (OAuth 가입에는 사용하지 않음)
export const sendEmailVerificationCode = async (body: EmailSendCodeRequest): Promise<void> => {
  const res = await fetch(`${API_BASE_URL}/users/email/send-code`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  const json: ApiResponse = await res.json();

  if (!res.ok) {
    throw new Error(json.message || '인증번호 발송에 실패했습니다.');
  }
};

export const verifyEmailCode = async (body: EmailVerifyCodeRequest): Promise<void> => {
  const res = await fetch(`${API_BASE_URL}/users/email/verify-code`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  const json: ApiResponse = await res.json();

  if (!res.ok) {
    throw new Error(json.message || '인증번호 확인에 실패했습니다.');
  }
};

// 비밀번호 재설정 인증번호 발송 — 계정 열거 방지를 위해 백엔드가 항상 동일한 성공 메시지를 반환함
export const requestPasswordResetCode = async (body: PasswordResetCodeRequest): Promise<void> => {
  const res = await fetch(`${API_BASE_URL}/auth/password-reset/code`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  const json: ApiResponse = await res.json();

  if (!res.ok) {
    throw new Error(json.message || '인증번호 발송에 실패했습니다.');
  }
};

// 비밀번호 재설정 인증번호 검증 — 성공 시 비밀번호 변경 API 전용 임시 토큰(resetToken)을 반환
export const verifyPasswordResetCode = async (
  body: PasswordResetVerifyRequest
): Promise<PasswordResetVerifyData> => {
  const res = await fetch(`${API_BASE_URL}/auth/password-reset/verify`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  const json: ApiResponse<PasswordResetVerifyData> = await res.json();

  if (!res.ok) {
    throw new Error(json.message || '인증번호 확인에 실패했습니다.');
  }
  return json.data!;
};

// 새 비밀번호로 변경 — resetToken은 AccessToken/RefreshToken과 무관한 일회성 임시 토큰
export const confirmPasswordReset = async (body: PasswordResetConfirmRequest): Promise<void> => {
  const res = await fetch(`${API_BASE_URL}/auth/password-reset/confirm`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  const json: ApiResponse = await res.json();

  if (!res.ok) {
    throw new Error(json.message || '비밀번호 변경에 실패했습니다.');
  }
};

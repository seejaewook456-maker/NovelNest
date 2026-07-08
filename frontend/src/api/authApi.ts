import type {
  LoginRequest,
  SignupRequest,
  LoginData,
  EmailSendCodeRequest,
  EmailVerifyCodeRequest,
} from '../types/auth';
import { API_BASE_URL } from './config';

// 백엔드 공통 응답 구조
interface ApiResponse<T = undefined> {
  message: string;
  data?: T;
}

export const login = async (body: LoginRequest): Promise<string> => {
  const res = await fetch(`${API_BASE_URL}/users/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  const json: ApiResponse<LoginData> = await res.json();

  if (!res.ok) {
    throw new Error(json.message || '로그인에 실패했습니다.');
  }

  // 백엔드 응답: { message: "로그인 성공", data: { accessToken: "..." } }
  return json.data!.accessToken;
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

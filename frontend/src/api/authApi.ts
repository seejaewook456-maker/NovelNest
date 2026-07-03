import type { LoginRequest, SignupRequest, LoginData } from '../types/auth';
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

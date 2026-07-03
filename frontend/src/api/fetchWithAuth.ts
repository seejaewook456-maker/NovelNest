import { getToken, removeToken } from '../utils/token';
import { API_BASE_URL } from './config';

interface ApiResponse<T = undefined> {
  message: string;
  data?: T;
}

// 인증 헤더를 포함한 fetch 래퍼 — 401 시 자동 로그아웃
// path는 '/users/login'처럼 API_BASE_URL 이후의 경로만 전달
export const fetchWithAuth = async <T>(
  path: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> => {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getToken()}`,
      ...(options.headers as Record<string, string>),
    },
  });

  // 토큰 만료 시 로그아웃 후 로그인 페이지로 이동
  if (res.status === 401) {
    removeToken();
    window.location.href = '/login';
    throw new Error('인증이 만료되었습니다. 다시 로그인해 주세요.');
  }

  const json: ApiResponse<T> = await res.json();

  if (!res.ok) {
    throw new Error(json.message || '요청 처리 중 오류가 발생했습니다.');
  }

  return json;
};

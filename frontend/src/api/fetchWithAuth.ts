import { getToken, getRefreshToken, saveTokens, clearTokens } from '../utils/token';
import { API_BASE_URL } from './config';

interface ApiResponse<T = undefined> {
  message: string;
  data?: T;
}

// Access Token 만료로 401을 받은 요청이 동시에 여러 개 발생해도
// Refresh API는 단 한 번만 호출되도록 진행 중인 재발급 요청을 공유한다.
let refreshPromise: Promise<string | null> | null = null;

const requestRefresh = async (): Promise<string | null> => {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;

  try {
    const res = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });

    if (!res.ok) return null;

    const json: ApiResponse<{ accessToken: string; refreshToken: string }> = await res.json();
    if (!json.data) return null;

    saveTokens(json.data.accessToken, json.data.refreshToken);
    return json.data.accessToken;
  } catch {
    return null;
  }
};

const refreshAccessToken = (): Promise<string | null> => {
  if (!refreshPromise) {
    refreshPromise = requestRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
};

const goToLoginExpired = (): void => {
  clearTokens();
  window.location.href = '/login?expired=true';
};

// 인증 헤더를 포함한 fetch 래퍼
// - Access Token 만료(401) 시 Refresh Token으로 자동 재발급 후 원래 요청을 한 번 재시도한다.
// - Refresh Token까지 만료/무효화된 경우에만 로그인 페이지로 이동한다.
// path는 '/users/login'처럼 API_BASE_URL 이후의 경로만 전달
export const fetchWithAuth = async <T>(
  path: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> => {
  const callApi = (token: string | null) =>
    fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        ...(options.headers as Record<string, string>),
      },
    });

  let res = await callApi(getToken());

  if (res.status === 401) {
    const newAccessToken = await refreshAccessToken();

    if (!newAccessToken) {
      goToLoginExpired();
      throw new Error('로그인이 만료되었습니다. 다시 로그인해주세요.');
    }

    res = await callApi(newAccessToken);

    if (res.status === 401) {
      goToLoginExpired();
      throw new Error('로그인이 만료되었습니다. 다시 로그인해주세요.');
    }
  }

  const json: ApiResponse<T> = await res.json();

  if (!res.ok) {
    throw new Error(json.message || '요청 처리 중 오류가 발생했습니다.');
  }

  return json;
};

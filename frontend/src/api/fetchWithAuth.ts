import { getToken, getRefreshToken, saveTokens, clearTokens } from '../utils/token';
import { notifySessionExpired } from '../state/sessionExpired';
import { API_BASE_URL } from './config';

interface ApiResponse<T = undefined> {
  message: string;
  code?: string;
  data?: T;
}

// fetch 자체가 실패한 경우(오프라인, 서버 다운, CORS 등)를 서버가 명시적으로
// refresh token을 거부한 경우와 구분하기 위한 에러 타입.
// 전자는 토큰이 여전히 유효할 수 있으므로 로그아웃 처리하면 안 된다.
export class NetworkError extends Error {}

// 서버가 명시적으로 거부한 응답(4xx/5xx)에서 던지는 에러 타입.
// 백엔드 공통 응답의 code(예: "USER_ALREADY_WITHDRAWN")를 그대로 담아,
// 호출부가 메시지 문자열 비교 없이 특정 상황을 안전하게 분기할 수 있게 한다.
export class ApiError extends Error {
  code?: string;

  constructor(message: string, code?: string) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
  }
}

// Access Token 만료로 401을 받은 요청이 동시에 여러 개 발생해도
// Refresh API는 단 한 번만 호출되도록 진행 중인 재발급 요청을 공유한다.
let refreshPromise: Promise<string | null> | null = null;

const requestRefresh = async (): Promise<string | null> => {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;

  let res: Response;
  try {
    res = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
  } catch {
    // 네트워크 오류: refresh token 자체가 무효화된 것이 아니므로 세션 만료로 취급하지 않는다.
    throw new NetworkError('네트워크 연결을 확인해주세요.');
  }

  // 서버가 명시적으로 거부한 경우에만 진짜 만료/무효로 판단한다.
  if (!res.ok) return null;

  const json: ApiResponse<{ accessToken: string; refreshToken: string }> = await res.json();
  if (!json.data) return null;

  saveTokens(json.data.accessToken, json.data.refreshToken);
  return json.data.accessToken;
};

const refreshAccessToken = (): Promise<string | null> => {
  if (!refreshPromise) {
    refreshPromise = requestRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
};

// Refresh Token까지 만료/무효화되어 더 이상 인증을 유지할 수 없는 경우 호출.
// 즉시 페이지를 이동시키지 않고, 토큰만 정리한 뒤 SessionExpiredModal에 안내를 위임한다
// (여러 요청이 동시에 이 경로를 타도 모달은 notifySessionExpired 내부 플래그로 한 번만 뜬다).
const handleSessionExpired = (): void => {
  clearTokens();
  notifySessionExpired();
};

// 인증 헤더를 포함한 fetch 래퍼
// - Access Token 만료(401) 시 Refresh Token으로 자동 재발급 후 원래 요청을 한 번 재시도한다.
// - Refresh Token까지 만료/무효화된 경우에만 세션 만료를 알린다.
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

  let res: Response;
  try {
    res = await callApi(getToken());
  } catch {
    throw new NetworkError('네트워크 연결을 확인해주세요.');
  }

  if (res.status === 401) {
    let newAccessToken: string | null;
    try {
      newAccessToken = await refreshAccessToken();
    } catch (err) {
      if (err instanceof NetworkError) throw err;
      throw new NetworkError('네트워크 연결을 확인해주세요.');
    }

    if (!newAccessToken) {
      handleSessionExpired();
      throw new Error('로그인이 만료되었습니다. 다시 로그인해주세요.');
    }

    try {
      res = await callApi(newAccessToken);
    } catch {
      throw new NetworkError('네트워크 연결을 확인해주세요.');
    }

    if (res.status === 401) {
      handleSessionExpired();
      throw new Error('로그인이 만료되었습니다. 다시 로그인해주세요.');
    }
  }

  const json: ApiResponse<T> = await res.json();

  if (!res.ok) {
    throw new ApiError(json.message || '요청 처리 중 오류가 발생했습니다.', json.code);
  }

  return json;
};

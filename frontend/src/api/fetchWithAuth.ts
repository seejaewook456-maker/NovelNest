import { getToken, getRefreshToken, saveTokens, clearTokens } from '../utils/token';
import { notifySessionExpired } from '../state/sessionExpired';
import { API_BASE_URL } from './config';
import { captureApiError } from '../lib/sentry';

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

// 경로만으로 안전하게 분류 가능한 경우에만 기능 태그를 붙인다 — 억지로 모든 경로를 분류하지 않는다.
// AI 관련 패턴을 먼저 검사해야 한다 (예: /episodes/{id}/summary는 episode가 아니라 ai_summary).
function deriveFeatureTag(path: string): string | undefined {
  if (path.includes('/character-extraction')) return 'ai_character';
  if (path.includes('/world-setting-extraction')) return 'ai_worldview';
  if (path.includes('/conflict-detection')) return 'ai_conflict';
  if (path.includes('/summary')) return 'ai_summary';
  if (path.includes('/chat')) return 'ai_chat';
  if (path.includes('/characters')) return 'character';
  if (path.includes('/world-settings')) return 'worldsetting';
  if (path.startsWith('/episodes')) return 'episode';
  if (path.startsWith('/novels')) return 'novel';
  if (path.startsWith('/auth') || path.startsWith('/users')) return 'auth';
  return undefined;
}

// 네트워크 실패(오프라인, 서버 다운, CORS 등)를 Sentry에 남기고 NetworkError로 던진다.
// 항상 던지므로 반환 타입을 never로 선언해, 호출부 이후 코드가 도달 불가능함을 타입 체커가 알게 한다.
function throwNetworkError(path: string, method: string): never {
  const error = new NetworkError('네트워크 연결을 확인해주세요.');
  captureApiError(error, { feature: deriveFeatureTag(path), path, method });
  throw error;
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
    throwNetworkError('/auth/refresh', 'POST');
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

  const method = options.method ?? 'GET';

  let res: Response;
  try {
    res = await callApi(getToken());
  } catch {
    throwNetworkError(path, method);
  }

  if (res.status === 401) {
    let newAccessToken: string | null;
    try {
      newAccessToken = await refreshAccessToken();
    } catch (err) {
      // NetworkError는 requestRefresh 내부(throwNetworkError)에서 이미 캡처된 뒤 올라온 것이므로
      // 여기서 다시 캡처하면 같은 오류가 두 번 전송된다 — 그대로 재전파만 한다.
      if (err instanceof NetworkError) throw err;
      throwNetworkError('/auth/refresh', 'POST');
    }

    if (!newAccessToken) {
      handleSessionExpired();
      throw new Error('로그인이 만료되었습니다. 다시 로그인해주세요.');
    }

    try {
      res = await callApi(newAccessToken);
    } catch {
      throwNetworkError(path, method);
    }

    if (res.status === 401) {
      handleSessionExpired();
      throw new Error('로그인이 만료되었습니다. 다시 로그인해주세요.');
    }
  }

  let json: ApiResponse<T>;
  try {
    json = await res.json();
  } catch (parseError) {
    // 응답이 JSON이 아닌 예상하지 못한 형태로 끝난 경우(예: 게이트웨이가 반환한 HTML 오류 페이지)
    captureApiError(parseError, { feature: deriveFeatureTag(path), path, method, status: res.status });
    throw parseError;
  }

  if (!res.ok) {
    const error = new ApiError(json.message || '요청 처리 중 오류가 발생했습니다.', json.code);
    // 400/401/403/404 등은 예상 가능한 비즈니스 오류이므로 수집하지 않고, 500 이상만 수집한다.
    if (res.status >= 500) {
      captureApiError(error, { feature: deriveFeatureTag(path), path, method, status: res.status });
    }
    throw error;
  }

  return json;
};

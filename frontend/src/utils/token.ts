import { resetSessionExpiredNotice } from '../state/sessionExpired';

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

export const saveTokens = (accessToken: string, refreshToken: string): void => {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  // 새 토큰이 저장됐다는 건 세션이 다시 유효해졌다는 뜻이므로, 다음 만료 때 안내 모달이 다시 뜨도록 리셋한다.
  resetSessionExpiredNotice();
};

export const getToken = (): string | null => {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
};

export const getRefreshToken = (): string | null => {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
};

export const clearTokens = (): void => {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
};

export const isLoggedIn = (): boolean => {
  return getToken() !== null;
};

// 회차 임시 저장(LocalStorage) 키를 사용자별로 구분하기 위해 JWT의 subject(이메일)를 읽는다.
// 서버 API를 추가하지 않고 이미 저장된 accessToken만으로 식별자를 얻기 위한 목적이며,
// 토큰 형식이 예상과 다르거나 파싱에 실패해도 앱 동작에 영향이 없도록 항상 안전하게 폴백한다.
export const getCurrentUserKey = (): string => {
  const token = getToken();
  if (!token) return 'anonymous';
  try {
    const payload = token.split('.')[1];
    if (!payload) return 'anonymous';
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
    const json = decodeURIComponent(
      atob(padded)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join('')
    );
    const claims = JSON.parse(json) as { sub?: string };
    return claims.sub || 'anonymous';
  } catch {
    return 'anonymous';
  }
};

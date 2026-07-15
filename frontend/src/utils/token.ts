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

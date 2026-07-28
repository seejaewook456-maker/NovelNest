import type { AuthMethod } from '../constants/analyticsEvents';

type OAuthProvider = Extract<AuthMethod, 'google' | 'kakao'>;

const STORAGE_KEY = 'ga_oauth_provider';

// Google/카카오 로그인 버튼은 백엔드 OAuth2 엔드포인트로 즉시 이동하는 <a> 태그라서,
// 로그인이 끝나고 돌아오는 콜백 페이지(OAuth2CallbackPage)는 어떤 제공자였는지 알 수 없다.
// 그래서 이동 직전(onClick)에 세션에 잠깐 표시해두고, 콜백 처리 시 GA4 login 이벤트의
// method 파라미터를 채우는 용도로만 사용한다.
export function markOAuthProvider(provider: OAuthProvider): void {
  sessionStorage.setItem(STORAGE_KEY, provider);
}

// 콜백 처리 시 한 번만 읽고 즉시 지운다 (새로고침 등으로 재사용되지 않도록).
export function consumeOAuthProvider(): OAuthProvider | null {
  const value = sessionStorage.getItem(STORAGE_KEY);
  sessionStorage.removeItem(STORAGE_KEY);
  return value === 'google' || value === 'kakao' ? value : null;
}

import { createBrowserRouter, Navigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import type { RouterState } from 'react-router-dom';
import { isLoggedIn } from '../utils/token';
import { initializeAnalytics, trackPageView } from '../lib/analytics';
import MainLayout from '../layouts/MainLayout';
import LandingPage from '../pages/LandingPage';
import LoginPage from '../pages/LoginPage';
import SignupPage from '../pages/SignupPage';
import ForgotPasswordPage from '../pages/ForgotPasswordPage';
import OAuth2CallbackPage from '../pages/OAuth2CallbackPage';
import NovelListPage from '../pages/NovelListPage';
import NovelCreatePage from '../pages/NovelCreatePage';
import NovelDetailPage from '../pages/NovelDetailPage';
import EpisodeListPage from '../pages/EpisodeListPage';
import EpisodeCreatePage from '../pages/EpisodeCreatePage';
import EpisodeDetailPage from '../pages/EpisodeDetailPage';
import CharacterPage from '../pages/CharacterPage';
import WorldSettingPage from '../pages/WorldSettingPage';
import MemoListPage from '../pages/MemoListPage';
import MemoCreatePage from '../pages/MemoCreatePage';
import MemoDetailPage from '../pages/MemoDetailPage';
import CharacterReviewPage from '../pages/CharacterReviewPage';
import WorldSettingReviewPage from '../pages/WorldSettingReviewPage';

// 로그인 상태가 아니면 /login으로 리다이렉트하는 가드
function PrivateRoute({ children }: { children: ReactNode }) {
  if (!isLoggedIn()) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/signup',
    element: <SignupPage />,
  },
  {
    path: '/forgot-password',
    element: <ForgotPasswordPage />,
  },
  {
    // Google OAuth 콜백: 백엔드가 리다이렉트하는 경로 (인증 불필요)
    path: '/oauth2/callback',
    element: <OAuth2CallbackPage />,
  },
  {
    // 인증이 필요한 모든 페이지를 MainLayout 아래에 배치
    element: (
      <PrivateRoute>
        <MainLayout />
      </PrivateRoute>
    ),
    children: [
      { path: '/novels', element: <NovelListPage /> },
      { path: '/novels/new', element: <NovelCreatePage /> },
      { path: '/novels/:novelId', element: <NovelDetailPage /> },
      { path: '/novels/:novelId/episodes', element: <EpisodeListPage /> },
      { path: '/novels/:novelId/episodes/new', element: <EpisodeCreatePage /> },
      { path: '/episodes/:episodeId', element: <EpisodeDetailPage /> },
      { path: '/episodes/:episodeId/character-review', element: <CharacterReviewPage /> },
      { path: '/episodes/:episodeId/world-setting-review', element: <WorldSettingReviewPage /> },
      { path: '/novels/:novelId/characters', element: <CharacterPage /> },
      { path: '/novels/:novelId/world-settings', element: <WorldSettingPage /> },
      { path: '/novels/:novelId/memos', element: <MemoListPage /> },
      { path: '/novels/:novelId/memos/new', element: <MemoCreatePage /> },
      { path: '/memos/:memoId', element: <MemoDetailPage /> },
    ],
  },
  {
    // 루트 접속 시 로그인 상태에 따라 분기: 로그인 상태면 작품 목록으로, 비로그인 상태면 랜딩 페이지를 보여준다.
    path: '/',
    element: isLoggedIn() ? <Navigate to="/novels" replace /> : <LandingPage />,
  },
]);

// createBrowserRouter(데이터 라우터) 구조라 <BrowserRouter> 하위에 페이지 추적용 컴포넌트를
// 따로 둘 root 레이아웃이 없다. router.subscribe로 위치 변화를 직접 구독하면 라우트 구성을
// 바꾸지 않고도 모든 경로의 이동을 추적할 수 있다.
//
// 이 시점에 GA4를 초기화하는 이유: main.tsx가 App을 import하면 ES 모듈 규칙상 이 파일의
// 최상위 코드가 main.tsx 본문(초기화 호출)보다 먼저 실행된다. 아래에서 최초 진입 페이지를
// 추적하기 전에 initializeAnalytics()를 직접 호출해 그 순서 문제를 피한다
// (initializeAnalytics는 중복 호출해도 한 번만 초기화되므로 안전하다).
initializeAnalytics();

// 매칭된 라우트의 path 패턴(:novelId 등 동적 세그먼트 포함)을 그대로 사용해
// `/novels/123` → `/novels/:novelId`처럼 정규화한다. 별도 정규식 없이 라우터 설정과 항상 일치한다.
function getNormalizedPagePath(state: RouterState): string {
  const lastMatch = state.matches[state.matches.length - 1];
  return lastMatch?.route.path ?? state.location.pathname;
}

// history 엔트리별 고유 key로 중복 여부를 판단한다. 같은 정규화 경로(`/novels/:novelId`)라도
// 실제로는 다른 작품으로 이동한 것일 수 있으므로, 정규화된 문자열이 아니라 location.key로
// "진짜 이동이 발생했는지"를 구분해야 동일 페이지 재방문이 각각 정상 집계된다.
let lastTrackedLocationKey: string | null = null;
function handleLocationChange(state: RouterState): void {
  if (state.location.key === lastTrackedLocationKey) return;
  lastTrackedLocationKey = state.location.key;
  trackPageView(getNormalizedPagePath(state));
}

// 최초 진입 페이지 추적 — router.subscribe는 이후의 상태 변화에만 반응하므로 별도 호출이 필요하다.
handleLocationChange(router.state);
router.subscribe(handleLocationChange);

import { createBrowserRouter, Navigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import { isLoggedIn } from '../utils/token';
import MainLayout from '../layouts/MainLayout';
import LoginPage from '../pages/LoginPage';
import SignupPage from '../pages/SignupPage';
import NovelListPage from '../pages/NovelListPage';
import NovelCreatePage from '../pages/NovelCreatePage';
import NovelDetailPage from '../pages/NovelDetailPage';
import EpisodeListPage from '../pages/EpisodeListPage';
import EpisodeCreatePage from '../pages/EpisodeCreatePage';
import EpisodeDetailPage from '../pages/EpisodeDetailPage';
import CharacterPage from '../pages/CharacterPage';
import WorldSettingPage from '../pages/WorldSettingPage';
import CharacterReviewPage from '../pages/CharacterReviewPage';

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
      { path: '/novels/:novelId/characters', element: <CharacterPage /> },
      { path: '/novels/:novelId/world-settings', element: <WorldSettingPage /> },
    ],
  },
  {
    // 루트 접속 시 로그인 상태에 따라 분기
    path: '/',
    element: isLoggedIn() ? <Navigate to="/novels" replace /> : <Navigate to="/login" replace />,
  },
]);

import { Outlet, useNavigate } from 'react-router-dom';
import { logout } from '../api/authApi';
import { clearTokens } from '../utils/token';
import Button from '../components/Button';

export default function MainLayout() {
  const navigate = useNavigate();

  const handleLogout = async () => {
    // 서버에 저장된 Refresh Token 무효화 시도 — 실패하더라도 로컬 로그아웃은 진행한다
    try {
      await logout();
    } catch {
      // 네트워크 오류 등으로 서버 로그아웃에 실패해도 사용자는 로그아웃되어야 하므로 무시
    }
    clearTokens();
    navigate('/login');
  };

  return (
    <div className="main-layout">
      <header className="header">
        <span className="header-logo" onClick={() => navigate('/novels')}>
          노벨네스트
        </span>
        <Button variant="ghost" size="sm" onClick={handleLogout}>
          로그아웃
        </Button>
      </header>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}

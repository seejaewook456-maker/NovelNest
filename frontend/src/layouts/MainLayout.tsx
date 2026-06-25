import { Outlet, useNavigate } from 'react-router-dom';
import { removeToken } from '../utils/token';
import Button from '../components/Button';

export default function MainLayout() {
  const navigate = useNavigate();

  const handleLogout = () => {
    removeToken();
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

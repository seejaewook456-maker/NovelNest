import { Outlet, useNavigate } from 'react-router-dom';
import { removeToken } from '../utils/token';

export default function MainLayout() {
  const navigate = useNavigate();

  const handleLogout = () => {
    removeToken();
    navigate('/login');
  };

  return (
    <div className="main-layout">
      <header className="header">
        <span className="header-title" onClick={() => navigate('/novels')} style={{ cursor: 'pointer' }}>
          작가의 AI 비서
        </span>
        <button className="btn-secondary" onClick={handleLogout}>
          로그아웃
        </button>
      </header>
      <main className="main-content">
        {/* 자식 라우트가 여기에 렌더링됨 */}
        <Outlet />
      </main>
    </div>
  );
}

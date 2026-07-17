import { Outlet, useNavigate } from 'react-router-dom';
import Button from '../components/Button';

export default function MainLayout() {
  const navigate = useNavigate();

  // 실제 서버 로그아웃 호출과 토큰 삭제는 LoginPage(?logout=1)에서 수행한다.
  // 여기서 미리 토큰을 지우면, 편집 중인 페이지의 "저장되지 않은 변경사항" 이동 차단이
  // 자동 저장을 시도하는 순간 이미 토큰이 없어 401(세션 만료)로 오인되고, 사용자가 이동을
  // "취소"해도 이미 지워진 토큰 때문에 강제 로그아웃되는 문제가 있었다.
  // navigate()만 먼저 호출해 이동 차단(useBlocker)이 정상적으로 개입할 기회를 준다.
  const handleLogout = () => {
    navigate('/login?logout=1');
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

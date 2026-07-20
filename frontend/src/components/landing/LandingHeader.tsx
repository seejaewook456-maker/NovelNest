import { Link, useNavigate } from 'react-router-dom';
import Button from '../Button';

// LoginPage의 브랜드 연필 아이콘과 동일한 컨셉의 인라인 SVG로, 로고 표기를 일관되게 유지한다.
function BrandMark() {
  return (
    <svg viewBox="0 0 64 64" width="26" height="26" aria-hidden="true">
      <g transform="translate(24,50) rotate(26) scale(0.82)">
        <path d="M-6.5,-48 A6.5,6.5 0 0 1 6.5,-48 L6.5,-11 L0,0 L-6.5,-11 Z" fill="#70492E" />
        <rect x="-6.5" y="-41" width="13" height="3" fill="#FFFFFF" />
        <path d="M-3.5,-15 L2.5,-9 L-3.5,-3 L-0.5,-9 Z" fill="#FFFFFF" />
      </g>
    </svg>
  );
}

export default function LandingHeader() {
  const navigate = useNavigate();

  return (
    <header className="landing-header">
      <div className="landing-header-inner">
        <Link to="/" className="landing-header-brand" aria-label="노벨네스트 홈으로 이동">
          <BrandMark />
          <span>노벨네스트</span>
        </Link>
        <div className="landing-header-actions">
          <Button variant="primary" size="sm" onClick={() => navigate('/login')}>
            무료로 시작하기
          </Button>
        </div>
      </div>
    </header>
  );
}
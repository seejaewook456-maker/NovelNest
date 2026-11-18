import { Link, useNavigate } from 'react-router-dom';
import Button from '../Button';
import ThemeToggle from '../ThemeToggle';

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
        <Link to="/" className="landing-header-brand" aria-label="NovelNest 홈으로 이동">
          <BrandMark />
          {/* 서비스명(NovelNest)을 이미지가 아닌 HTML 텍스트로 노출해, 검색엔진·외부 심사(AWS Activate 등)에서
              서비스 이름을 명확히 확인할 수 있게 한다. */}
          <span>NovelNest</span>
        </Link>
        <nav className="landing-header-actions" aria-label="시작하기">
          <ThemeToggle />
          <Button variant="primary" size="sm" onClick={() => navigate('/login')}>
            무료로 시작하기
          </Button>
        </nav>
      </div>
    </header>
  );
}
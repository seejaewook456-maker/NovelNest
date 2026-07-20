import { useNavigate } from 'react-router-dom';
import Button from '../Button';
import LandingImage from './LandingImage';

interface HeroSectionProps {
  onExploreFeatures: () => void;
}

export default function HeroSection({ onExploreFeatures }: HeroSectionProps) {
  const navigate = useNavigate();

  return (
    <section className="landing-hero">
      <div className="landing-hero-copy">
        <h1 className="landing-hero-title">
          세계관 설정은 AI가 기억해줍니다. 당신은 이야기에만 집중하세요.
        </h1>
        <p className="landing-hero-desc">
          등장인물, 세계관, 회차를 AI가 분석하고, 관리하고, 기억합니다. 설정 충돌까지 자동으로 확인하며
          더 완성도 높은 이야기를 만들어보세요.
        </p>
        <div className="landing-hero-actions">
          <Button variant="primary" onClick={() => navigate('/login')}>
            무료로 시작하기
          </Button>
          <Button variant="secondary" onClick={onExploreFeatures}>
            기능 살펴보기
          </Button>
        </div>
      </div>
      <div className="landing-hero-visual">
        <div className="landing-browser-frame">
          <div className="landing-browser-frame-bar" aria-hidden="true">
            <span />
            <span />
            <span />
          </div>
          <LandingImage
            src="/landing/hero-dashboard.png"
            alt="노벨네스트 작품 대시보드 화면"
            className="landing-browser-frame-img"
            eager
          />
        </div>
      </div>
    </section>
  );
}
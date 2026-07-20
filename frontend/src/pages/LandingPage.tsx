import { useCallback } from 'react';
import LandingHeader from '../components/landing/LandingHeader';
import HeroSection from '../components/landing/HeroSection';
import ProblemSolutionSection from '../components/landing/ProblemSolutionSection';
import FeatureShowcaseSection from '../components/landing/FeatureShowcaseSection';
import HowItWorksSection from '../components/landing/HowItWorksSection';
import AudienceSection from '../components/landing/AudienceSection';
import FinalCtaSection from '../components/landing/FinalCtaSection';
import LandingFooter from '../components/landing/LandingFooter';

export default function LandingPage() {
  const scrollToFeatures = useCallback(() => {
    document.getElementById('features')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, []);

  return (
    <div className="landing-page">
      <LandingHeader />
      <main>
        <HeroSection onExploreFeatures={scrollToFeatures} />
        <ProblemSolutionSection />
        <FeatureShowcaseSection />
        <HowItWorksSection />
        <AudienceSection />
        <FinalCtaSection />
      </main>
      <LandingFooter />
    </div>
  );
}
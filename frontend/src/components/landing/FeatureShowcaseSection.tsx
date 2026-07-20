import LandingImage from './LandingImage';

interface FeatureItem {
  image: string;
  alt: string;
  title: string;
  desc: string;
}

const FEATURES: FeatureItem[] = [
  {
    image: '/landing/episode-page.png',
    alt: '회차 작성 및 관리 화면',
    title: '회차를 한곳에서 작성하고 관리하세요',
    desc: '작품별 회차를 체계적으로 관리하고, 작성한 본문을 언제든 확인하고 수정할 수 있습니다.',
  },
  {
    image: '/landing/summary.png',
    alt: 'AI 회차 요약 화면',
    title: '긴 회차도 핵심만 빠르게 확인하세요',
    desc: 'AI가 회차의 주요 사건과 흐름을 요약해 이전 내용을 빠르게 파악할 수 있습니다.',
  },
  {
    image: '/landing/character-page.png',
    alt: '등장인물 관리 화면',
    title: '등장인물의 특징과 관계를 놓치지 마세요',
    desc: 'AI가 회차에서 등장인물을 추출하고 성격, 특징, 역할 등의 정보를 정리합니다.',
  },
  {
    image: '/landing/world-page.png',
    alt: '세계관 관리 화면',
    title: '복잡한 세계관도 체계적으로 정리하세요',
    desc: '장소, 조직, 능력, 아이템 등 작품의 세계관 정보를 카테고리별로 관리할 수 있습니다.',
  },
  {
    image: '/landing/conflict.png',
    alt: '설정 충돌 감지 화면',
    title: '연재가 길어져도 설정을 일관되게 유지하세요',
    desc: '새로운 회차의 내용이 기존 등장인물이나 세계관 설정과 충돌하는지 AI가 확인합니다.',
  },
  {
    image: '/landing/ai-chat.png',
    alt: '작품 기반 AI 챗봇 화면',
    title: '내 작품에 대해 AI에게 질문하세요',
    desc: '저장된 회차 요약, 등장인물, 세계관 정보를 기반으로 작품에 관한 질문에 답변합니다.',
  },
];

export default function FeatureShowcaseSection() {
  return (
    <section className="landing-section" id="features">
      <div className="landing-section-head">
        <h2>핵심 기능</h2>
        <p>글쓰기에만 집중할 수 있도록, 나머지는 AI가 정리합니다.</p>
      </div>
      <div className="landing-feature-list">
        {FEATURES.map((feature, index) => (
          <div key={feature.title} className={`landing-feature-row${index % 2 === 1 ? ' reverse' : ''}`}>
            <div className="landing-feature-visual">
              <div className="landing-browser-frame">
                <div className="landing-browser-frame-bar" aria-hidden="true">
                  <span />
                  <span />
                  <span />
                </div>
                <LandingImage src={feature.image} alt={feature.alt} className="landing-browser-frame-img" />
              </div>
            </div>
            <div className="landing-feature-copy">
              <h3>{feature.title}</h3>
              <p>{feature.desc}</p>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
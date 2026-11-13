import LandingImage from './LandingImage';

// 테마별 캡처 한 쌍. 파일은 public/landing/{name}-light.png / {name}-dark.png 규칙으로 저장한다.
// width/height는 이미지 박스의 기준 비율이며, 라이트/다크 중 세로로 더 긴 쪽의 픽셀 크기를 사용한다.
// (가로가 더 긴 쪽은 같은 박스 안에서 위아래로 몇 px 여백이 생길 뿐 잘리지 않는다)
interface FeatureScreenshot {
  name: string;
  alt: string;
  width: number;
  height: number;
}

interface FeatureItem {
  // 한 기능에 캡처가 여러 장이면 각각 별도 프레임(박스)으로 감싸 위에서 아래로 간격을 두고 쌓는다.
  screenshots: FeatureScreenshot[];
  // 프레임 최대 너비(px). 생략하면 캡처 원본 너비로 제한해 작은 캡처가 확대되어 흐려지지 않게 한다.
  frameMaxWidth?: number;
  title: string;
  desc: string;
}

const FEATURES: FeatureItem[] = [
  {
    screenshots: [
      { name: 'episode-write', alt: 'AI 웹소설 회차 작성 화면', width: 1012, height: 822 },
    ],
    title: '작품 정보를 참고하며 회차를 작성하세요',
    desc: '저장된 메모, 이전 회차, 인물 및 세계관 정보 등을 실시간으로 확인하면서 새로운 회차 본문을 작성할 수 있습니다.',
  },
  {
    screenshots: [
      { name: 'episode-summary', alt: '회차 요약 결과', width: 512, height: 567 },
    ],
    title: '긴 회차도 핵심만 빠르게 확인하세요',
    desc: 'AI가 회차의 주요 사건과 흐름을 요약해 이전 내용을 빠르게 파악할 수 있습니다.',
  },
  {
    screenshots: [
      { name: 'character-extract', alt: 'AI 등장인물 추출 화면', width: 947, height: 721 },
      { name: 'character-info', alt: '등록된 등장인물 정보 카드', width: 920, height: 282 },
    ],
    title: '등장인물의 특징과 관계를 놓치지 마세요',
    desc: 'AI가 회차 본문에서 언급된 등장인물을 추출하고 역할, 성격, 말투 등의 정보를 정리하여 저장합니다.',
  },
  {
    screenshots: [
      { name: 'world-extract', alt: 'AI 세계관 설정 추출 화면', width: 912, height: 692 },
      { name: 'world-info', alt: '등록된 세계관 설정 카드', width: 902, height: 236 },
    ],
    title: '복잡한 세계관도 체계적으로 정리하세요',
    // \n은 화면에서 줄바꿈으로 표시된다(.landing-feature-copy p의 white-space: pre-line)
    desc: 'AI가 회차 본문에서 언급된 세계관 설정을 추출하고 저장합니다.\n장소, 조직, 능력, 아이템 등 작품의 세계관 정보를 카테고리별로\n관리할 수 있습니다.',
  },
  {
    screenshots: [
      { name: 'conflict', alt: 'AI 설정 충돌 감지 화면', width: 436, height: 747 },
    ],
    // 세로로 긴 캡처라 원본 너비 그대로 두면 행 높이가 과하게 커지므로 조금 줄여서 보여준다.
    frameMaxWidth: 400,
    title: '연재가 길어져도 설정을 일관되게 유지하세요',
    desc: '새로운 회차의 내용이 기존 등장인물 정보나 세계관 설정과 모순되는 부분이 있는지 점검합니다.',
  },
  {
    screenshots: [
      { name: 'chatbot', alt: '작품 기반 AI 챗봇 화면', width: 937, height: 777 },
    ],
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
        {FEATURES.map((feature, index) => {
          const frameMaxWidth =
            feature.frameMaxWidth ?? Math.min(...feature.screenshots.map((shot) => shot.width));

          return (
            <div key={feature.title} className={`landing-feature-row${index % 2 === 1 ? ' reverse' : ''}`}>
              <div className="landing-feature-visual">
                {feature.screenshots.map((shot) => {
                  // 세로형 캡처는 기존 최대 높이(560px) 제한을 풀어야 좌우 여백 없이 꽉 차게 보인다.
                  // 대신 frameMaxWidth로 너비를 제한해 전체 높이를 조절한다.
                  const classNames = ['landing-browser-frame-img'];
                  if (shot.height > shot.width) classNames.push('landing-browser-frame-img-tall');

                  // 캡처마다 별도의 프레임(박스)으로 감싼다. 같은 기능의 프레임은 너비를 동일하게 맞춰 정렬을 유지한다.
                  return (
                    <div key={shot.name} className="landing-browser-frame" style={{ maxWidth: frameMaxWidth }}>
                      <div className="landing-browser-frame-bar" aria-hidden="true">
                        <span />
                        <span />
                        <span />
                      </div>
                      <LandingImage
                        lightSrc={`/landing/${shot.name}-light.png`}
                        darkSrc={`/landing/${shot.name}-dark.png`}
                        alt={shot.alt}
                        width={shot.width}
                        height={shot.height}
                        className={classNames.join(' ')}
                      />
                    </div>
                  );
                })}
              </div>
              <div className="landing-feature-copy">
                <h3>{feature.title}</h3>
                <p>{feature.desc}</p>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}

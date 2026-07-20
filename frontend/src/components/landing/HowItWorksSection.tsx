const STEPS = [
  { step: '1', title: '작품 만들기', desc: '제목과 기본 정보를 입력해 새로운 작품을 생성' },
  { step: '2', title: '회차 작성하기', desc: '작품의 회차를 작성하고 저장' },
  { step: '3', title: 'AI로 분석하기', desc: '회차 요약, 등장인물·세계관 추출, 설정 충돌 감지 실행' },
];

export default function HowItWorksSection() {
  return (
    <section className="landing-section">
      <div className="landing-section-head">
        <h2>이렇게 사용하세요</h2>
      </div>
      <ol className="landing-steps">
        {STEPS.map((s) => (
          <li key={s.step} className="landing-step">
            <span className="landing-step-num" aria-hidden="true">
              {s.step}
            </span>
            <h3>{s.title}</h3>
            <p>{s.desc}</p>
          </li>
        ))}
      </ol>
    </section>
  );
}
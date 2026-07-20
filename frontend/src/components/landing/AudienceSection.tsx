const AUDIENCE = [
  '첫 작품을 준비하는 웹소설 지망 작가',
  '장편 작품을 연재하는 작가',
  '등장인물과 세계관 설정이 복잡한 작품의 작가',
  '이전 회차와 설정을 자주 다시 확인하는 작가',
];

export default function AudienceSection() {
  return (
    <section className="landing-section">
      <div className="landing-section-head">
        <h2>이런 분들에게 추천합니다</h2>
      </div>
      <ul className="landing-audience-grid">
        {AUDIENCE.map((item) => (
          <li key={item} className="landing-audience-card">
            {item}
          </li>
        ))}
      </ul>
    </section>
  );
}
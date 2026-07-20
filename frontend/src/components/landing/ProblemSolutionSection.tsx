const PROBLEMS = [
  '등장인물의 설정을 기억하기 어렵다.',
  '이전 회차의 내용을 다시 찾는 데 시간이 걸린다.',
  '세계관과 인물 정보가 여러 메모에 흩어진다.',
  '장기 연재 중 기존 설정과 충돌할 수 있다.',
];

const SOLUTIONS = [
  'AI가 등장인물과 세계관 정보를 자동으로 추출하여 저장',
  '회차별 AI 요약 제공',
  '작품 정보를 기억하고 있는 AI와의 채팅을 통한 실시간 질의응답',
  '이전 회차와 현재 회차의 설정 충돌 감지',
];

export default function ProblemSolutionSection() {
  return (
    <section className="landing-section">
      <div className="landing-section-head">
        <h2>이런 어려움, 익숙하지 않으신가요?</h2>
      </div>
      <div className="landing-compare-grid">
        <div className="landing-compare-col landing-compare-problem">
          <h3>기존 집필 과정의 문제</h3>
          <ul>
            {PROBLEMS.map((item) => (
              <li key={item}>
                <span className="landing-compare-mark landing-compare-mark-x" aria-hidden="true">
                  ✕
                </span>
                {item}
              </li>
            ))}
          </ul>
        </div>
        <div className="landing-compare-col landing-compare-solution">
          <h3>노벨네스트의 해결 방식</h3>
          <ul>
            {SOLUTIONS.map((item) => (
              <li key={item}>
                <span className="landing-compare-mark landing-compare-mark-check" aria-hidden="true">
                  ✓
                </span>
                {item}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}
// AI 도구 박스/챗봇 박스 하단에 반복 표시되는 "매일 00:00 충전" 안내 문구.
// 두 곳(EpisodeDetailPage, AiChatPanel)에서 동일한 문구+아이콘을 중복 작성하지 않도록 공통화했다.
// 아이콘은 이모지 대신 CDN 의존 없는 인라인 SVG(시계 모양, Warm Brown 브랜드 색 상속)로 구현해
// OS/브라우저에 따라 다르게 보이는 이모지보다 더 모던하고 일관된 느낌을 준다.
export default function AiUsageRechargeNote() {
  return (
    <p className="ai-usage-recharge-note">
      <svg
        className="ai-usage-recharge-note-icon"
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <circle cx="12" cy="12" r="9" />
        <polyline points="12 7 12 12 15.5 14" />
      </svg>
      AI 도구 이용권은 매일 00:00에 충전됩니다.
    </p>
  );
}

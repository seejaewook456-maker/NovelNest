import type { AiUsageDetail } from '../types/aiUsage';

interface AiUsageHintProps {
  detail: AiUsageDetail | undefined;
  loading: boolean;
  error: string | null;
  depletedMessage: string;
}

// AI 기능 실행 버튼 근처에 "오늘 남은 횟수"를 표시하는 공통 컴포넌트.
// 회차 상세 페이지의 AI 도구 4종 카드와 AiChatPanel(3개 페이지)이 모두 이 컴포넌트를 사용해,
// 로딩/에러/잔여 0 상태를 항상 같은 방식으로 보여준다.
// 잔여 횟수는 라벨과 분리된 뱃지로 표시해 숫자가 한눈에 들어오도록 한다.
export default function AiUsageHint({ detail, loading, error, depletedMessage }: AiUsageHintProps) {
  // 아직 한 번도 값을 못 받아온 상태(최초 로딩 또는 이전 조회 실패) — 잘못된 기본값(예: 무제한처럼
  // 보이는 빈 화면) 대신 명확한 안내를 보여준다.
  if (loading && !detail) {
    return <p className="ai-usage-hint ai-usage-hint-muted">사용량 확인 중...</p>;
  }
  if (error || !detail) {
    return (
      <p className="ai-usage-hint ai-usage-hint-error">
        사용 가능 횟수를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.
      </p>
    );
  }

  const depleted = detail.remaining <= 0;
  return (
    <div className="ai-usage-hint">
      <span className="ai-usage-hint-label">오늘 남은 횟수</span>
      <span className={`ai-usage-hint-count${depleted ? ' ai-usage-hint-count-depleted' : ''}`}>
        {detail.remaining} / {detail.limit}회
      </span>
      {depleted && <span className="ai-usage-hint-depleted-text">{depletedMessage}</span>}
    </div>
  );
}

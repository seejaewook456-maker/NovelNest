// GET /api/ai/usage/daily 응답 — 백엔드 AiDailyUsageResponseDto와 1:1 대응.
// 프론트에서 제한 계산을 다시 하지 않고, 이 값(특히 remaining)을 그대로 표시에 사용한다.
export interface AiUsageDetail {
  used: number;
  remaining: number;
  limit: number;
}

export interface AiDailyUsage {
  usageDate: string;
  timezone: string;
  nextResetAt: string;
  summary: AiUsageDetail;
  conflict: AiUsageDetail;
  character: AiUsageDetail;
  worldview: AiUsageDetail;
  chat: AiUsageDetail;
}

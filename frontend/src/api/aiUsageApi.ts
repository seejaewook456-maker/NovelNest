import type { AiDailyUsage } from '../types/aiUsage';
import { fetchWithAuth } from './fetchWithAuth';

// 오늘(Asia/Seoul 기준) AI 기능별/전체 사용 현황 조회 — 조회 자체는 서버에서도 사용 횟수에 포함되지 않는다.
export const getDailyAiUsage = async (): Promise<AiDailyUsage> => {
  const json = await fetchWithAuth<AiDailyUsage>('/ai/usage/daily');
  return json.data!;
};

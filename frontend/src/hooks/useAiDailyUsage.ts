import { useEffect, useSyncExternalStore } from 'react';
import {
  ensureAiUsageLoaded,
  fetchAiUsage,
  getAiUsageState,
  subscribeAiUsage,
} from '../state/aiUsageStore';
import type { AiDailyUsage } from '../types/aiUsage';

interface UseAiDailyUsageResult {
  usage: AiDailyUsage | null;
  loading: boolean;
  error: string | null;
  // AI 실행 성공/실패 직후 호출해 최신값으로 다시 조회한다.
  refetch: () => Promise<void>;
}

// AI 사용량 조회 공통 훅 — 회차 상세 페이지(AI 도구 4종)와 AiChatPanel(3개 페이지)이 모두 이 훅
// 하나만 사용한다. 내부적으로 aiUsageStore를 구독하므로, 여러 컴포넌트가 동시에 마운트돼 있어도
// 조회는 한 번만 일어나고, 한 곳에서 refetch()하면 전부 같은 최신값으로 동시에 갱신된다.
export function useAiDailyUsage(): UseAiDailyUsageResult {
  const state = useSyncExternalStore(subscribeAiUsage, getAiUsageState);

  useEffect(() => {
    ensureAiUsageLoaded();
  }, []);

  return { ...state, refetch: fetchAiUsage };
}

import { getDailyAiUsage } from '../api/aiUsageApi';
import type { AiDailyUsage } from '../types/aiUsage';

// AI 사용량은 회차 상세 페이지의 AI 도구 박스와 AiChatPanel(작품 상세/회차 작성/회차 수정 3곳)이
// 동시에 화면에 보일 수 있다. 각자 useAiDailyUsage()를 호출해도 중복 조회하지 않고, 한쪽에서
// AI 기능을 실행해 갱신하면 다른 곳도 즉시 최신값을 보도록 sessionExpired.ts와 같은 방식의
// 모듈 단위 pub-sub 저장소로 값을 공유한다.
type Listener = () => void;

export interface AiUsageState {
  usage: AiDailyUsage | null;
  loading: boolean;
  error: string | null;
}

let state: AiUsageState = { usage: null, loading: false, error: null };
let inFlight: Promise<void> | null = null;
const listeners = new Set<Listener>();

export const subscribeAiUsage = (listener: Listener): (() => void) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

export const getAiUsageState = (): AiUsageState => state;

const setState = (next: Partial<AiUsageState>): void => {
  state = { ...state, ...next };
  listeners.forEach((listener) => listener());
};

// AI 실행 성공/실패 직후 등, 최신값을 강제로 다시 가져와야 할 때 호출한다.
// 동시에 여러 곳에서 호출돼도 진행 중인 요청 하나만 공유한다.
export const fetchAiUsage = (): Promise<void> => {
  if (inFlight) return inFlight;

  setState({ loading: true, error: null });
  inFlight = getDailyAiUsage()
    .then((usage) => {
      setState({ usage, loading: false, error: null });
    })
    .catch((err) => {
      setState({
        usage: null,
        loading: false,
        error:
          err instanceof Error
            ? err.message
            : '사용 가능 횟수를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.',
      });
    })
    .finally(() => {
      inFlight = null;
    });

  return inFlight;
};

// 이미 값이 있거나 조회 중이면 재사용하고, 처음일 때만 새로 조회한다(컴포넌트 마운트마다 중복 호출 방지).
export const ensureAiUsageLoaded = (): void => {
  if (state.usage || state.loading) return;
  void fetchAiUsage();
};

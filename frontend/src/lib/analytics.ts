import ReactGA from 'react-ga4';
import type { AnalyticsEventName } from '../constants/analyticsEvents';

type AnalyticsEventParams = Record<string, string | number | boolean>;

let isInitialized = false;

// 앱 시작 시 한 번만 호출한다.
// VITE_GA_MEASUREMENT_ID가 없으면(로컬 개발 환경 등) GA4를 초기화하지 않고 경고만 남긴다.
// 이미 초기화된 상태에서 다시 호출해도 중복 초기화되지 않는다.
export function initializeAnalytics(): void {
  if (isInitialized) return;

  const measurementId = import.meta.env.VITE_GA_MEASUREMENT_ID;
  if (!measurementId) {
    console.warn('[analytics] VITE_GA_MEASUREMENT_ID가 설정되지 않아 GA4를 초기화하지 않습니다.');
    return;
  }

  // 자동 page_view는 사용하지 않는다 — React Router 쪽에서 라우트 패턴으로 정규화한 경로를 직접 전송한다.
  ReactGA.initialize(measurementId, {
    gaOptions: { send_page_view: false },
  });
  isInitialized = true;
}

// page_view 이벤트를 전송한다. 동적 라우트는 호출하는 쪽(React Router 연동부)에서
// `/novels/:novelId`처럼 정규화한 경로를 넘겨줘야 한다.
export function trackPageView(pagePath: string): void {
  if (!isInitialized) return;
  ReactGA.send({ hitType: 'pageview', page: pagePath });
}

// 모든 커스텀 이벤트는 이 함수를 통해서만 전송한다 (ReactGA.event를 여러 곳에서 직접 호출하지 않는다).
// params에는 개인정보 및 사용자가 작성한 콘텐츠(제목/본문/AI 응답 등)를 절대 담지 않는다.
export function trackEvent(eventName: AnalyticsEventName, params?: AnalyticsEventParams): void {
  if (!isInitialized) return;
  ReactGA.event(eventName, params);
}

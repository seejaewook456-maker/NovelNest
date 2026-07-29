import * as Sentry from '@sentry/react';

let isInitialized = false;

// 앱 시작 시(React 렌더링 전) 한 번만 호출한다.
// - 운영 빌드(import.meta.env.PROD)가 아니면 초기화하지 않는다 — 로컬 개발/테스트에서는 항상 비활성화.
// - VITE_SENTRY_DSN이 없으면 초기화하지 않는다.
// - 이미 초기화된 상태에서 다시 호출해도(StrictMode 등) 중복 초기화되지 않는다.
// - 초기화 중 예외가 발생해도 앱 실행을 막지 않도록 전체를 try/catch로 감싼다.
//
// Source Map 업로드(@sentry/vite-plugin), 성능 모니터링(tracesSampleRate/BrowserTracing),
// 분산 추적, Sentry Replay, Release 관리는 이번 작업 범위가 아니므로 의도적으로 추가하지 않는다.
export function initializeSentry(): void {
  if (isInitialized || !import.meta.env.PROD) {
    return;
  }

  const dsn = import.meta.env.VITE_SENTRY_DSN;
  if (!dsn) {
    return;
  }

  try {
    Sentry.init({
      dsn,
      environment: 'production',
      sendDefaultPii: false,
      // integrations/tracesSampleRate를 지정하지 않으면 SDK 기본 integration만 활성화된다
      // (처리되지 않은 예외·Promise rejection 자동 수집). Replay·BrowserTracing은 별도로
      // 추가해야만 켜지는 옵트인 기능이라, 여기서 아무것도 추가하지 않는 것 자체가
      // "미적용"을 보장하는 방법이다.
      beforeSend,
      beforeBreadcrumb,
    });
    isInitialized = true;
  } catch (error) {
    console.warn('[sentry] 초기화에 실패했습니다.', error);
  }
}

// 쿼리스트링을 제거한다 — 검색어 등 사용자 입력이 URL에 실려 전송되는 것을 막기 위함.
// 예) /api/novels/1?keyword=검색어 → /api/novels/1
export function stripQueryString(url: string): string {
  const queryIndex = url.indexOf('?');
  return queryIndex === -1 ? url : url.slice(0, queryIndex);
}

interface ApiErrorContext {
  // 안전하게 분류 가능한 경우에만 채운다 (auth/novel/episode/ai_summary 등) — 억지로 채우지 않는다.
  feature?: string;
  method?: string;
  path: string;
  status?: number;
}

// API 계층(fetchWithAuth)에서만 호출하는 공통 오류 수집 함수 — 여러 API 함수에 중복 코드를
// 추가하지 않도록 이 함수 하나로 캡처 로직을 모은다.
// Context에는 기능 태그 / HTTP 상태 / 메서드 / 쿼리스트링을 제거한 경로 / 현재 화면 경로만 담고,
// 요청·응답 본문, 사용자 입력, 토큰, 사용자 식별 정보는 절대 포함하지 않는다.
export function captureApiError(error: unknown, context: ApiErrorContext): void {
  Sentry.captureException(error, {
    tags: context.feature ? { feature: context.feature } : undefined,
    contexts: {
      api: {
        method: context.method,
        path: stripQueryString(context.path),
        status: context.status,
        page: window.location.pathname,
      },
    },
  });
}

const SENSITIVE_HEADER_KEYS = new Set(['authorization', 'cookie']);

function sanitizeHeaders(
  headers: Record<string, string> | undefined
): Record<string, string> | undefined {
  if (!headers) return headers;
  const sanitized: Record<string, string> = {};
  for (const [key, value] of Object.entries(headers)) {
    if (SENSITIVE_HEADER_KEYS.has(key.toLowerCase())) continue;
    sanitized[key] = value;
  }
  return sanitized;
}

// fetch/xhr breadcrumb의 url, navigation breadcrumb의 to/from에서 쿼리스트링을 제거한다.
// (예: React Router가 OAuth 콜백 경로로 이동할 때 남는 /oauth2/callback?token=...을 그대로 두면
//  브레드크럼에 토큰이 실려 전송될 수 있다.)
function sanitizeBreadcrumb(breadcrumb: Sentry.Breadcrumb): Sentry.Breadcrumb {
  if (!breadcrumb.data) return breadcrumb;

  const data = { ...breadcrumb.data };
  if (typeof data.url === 'string') data.url = stripQueryString(data.url);
  if (typeof data.to === 'string') data.to = stripQueryString(data.to);
  if (typeof data.from === 'string') data.from = stripQueryString(data.from);
  delete data.request_body;
  delete data.response_body;

  return { ...breadcrumb, data };
}

// console.* 호출은 사용자 콘텐츠나 오류 메시지를 그대로 담을 수 있어 breadcrumb에서 제외한다.
function beforeBreadcrumb(breadcrumb: Sentry.Breadcrumb): Sentry.Breadcrumb | null {
  if (breadcrumb.category === 'console') return null;
  return sanitizeBreadcrumb(breadcrumb);
}

// Sentry로 전송되기 직전 마지막 방어선 — beforeBreadcrumb를 통과했더라도 한 번 더 제거한다.
function beforeSend(event: Sentry.ErrorEvent): Sentry.ErrorEvent | null {
  if (event.request) {
    delete event.request.data;
    delete event.request.cookies;
    delete event.request.query_string;
    if (event.request.url) {
      event.request.url = stripQueryString(event.request.url);
    }
    event.request.headers = sanitizeHeaders(event.request.headers);
  }

  if (event.user) {
    delete event.user.email;
    delete event.user.username;
    delete event.user.ip_address;
  }

  if (event.breadcrumbs) {
    event.breadcrumbs = event.breadcrumbs
      .filter((breadcrumb) => breadcrumb.category !== 'console')
      .map(sanitizeBreadcrumb);
  }

  return event;
}

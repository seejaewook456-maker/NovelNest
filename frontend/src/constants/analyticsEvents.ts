// GA4로 전송하는 이벤트명 상수 모음.
// 오타로 인해 GA4에 잘못된 이벤트명이 쌓이는 것을 막기 위해, 이벤트를 전송할 때는
// 항상 이 상수를 통해서만 이름을 참조한다 (문자열 리터럴 직접 사용 금지).
export const ANALYTICS_EVENTS = {
  // Google 권장 이벤트
  SIGN_UP: 'sign_up',
  LOGIN: 'login',

  // 작품/회차 관리
  NOVEL_CREATE: 'novel_create',
  EPISODE_CREATE: 'episode_create',
  EPISODE_UPDATE: 'episode_update',
  EPISODE_COPY: 'episode_copy',
  MEMO_CREATE: 'memo_create',
  MEMO_UPDATE: 'memo_update',

  // AI 도구
  AI_SUMMARY_RUN: 'ai_summary_run',
  AI_CONFLICT_CHECK_RUN: 'ai_conflict_check_run',
  AI_CHARACTER_EXTRACT_RUN: 'ai_character_extract_run',
  AI_WORLDVIEW_EXTRACT_RUN: 'ai_worldview_extract_run',
  AI_CHAT_MESSAGE_SEND: 'ai_chat_message_send',

  // 계정
  ACCOUNT_DELETE: 'account_delete',
} as const;

export type AnalyticsEventName = (typeof ANALYTICS_EVENTS)[keyof typeof ANALYTICS_EVENTS];

// sign_up / login 이벤트의 인증 수단 구분 값 (GA4 이벤트 파라미터 method에 사용)
export const AUTH_METHOD = {
  EMAIL: 'email',
  GOOGLE: 'google',
  KAKAO: 'kakao',
} as const;

export type AuthMethod = (typeof AUTH_METHOD)[keyof typeof AUTH_METHOD];

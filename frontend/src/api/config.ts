// REST API 호출용 base URL — 개발: /api(Vite proxy), 운영: https://api.novelnestia.com/api
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

// OAuth2 로그인 시작 등 /api prefix 없이 백엔드 루트로 직접 이동해야 할 때 사용
export const BACKEND_BASE_URL = import.meta.env.VITE_BACKEND_BASE_URL;

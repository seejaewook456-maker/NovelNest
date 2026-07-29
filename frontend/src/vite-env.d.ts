/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_BACKEND_BASE_URL: string;
  readonly VITE_GA_MEASUREMENT_ID?: string;
  readonly VITE_CLARITY_PROJECT_ID?: string;
  readonly VITE_SENTRY_DSN?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

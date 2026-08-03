export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'theme';

// aiUsageStore.ts / sessionExpired.ts와 동일한 모듈 단위 pub-sub 패턴.
// 헤더의 ThemeToggle, 작품 목록 페이지의 테마 라디오 등 여러 곳에서 동시에 구독해도
// 한 곳에서 setTheme()하면 전부 즉시 같은 값으로 갱신된다.
type Listener = () => void;

const listeners = new Set<Listener>();

// 시스템 설정 동기화 없이, 사용자가 직접 선택하지 않았다면 항상 라이트로 시작한다.
const readStoredTheme = (): Theme => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'dark') return 'dark';
  } catch {
    // localStorage 접근 불가(프라이빗 모드 등) — 기본값(라이트) 유지
  }
  return 'light';
};

let theme: Theme = readStoredTheme();

// index.html의 FOUC 방지 인라인 스크립트가 초기 data-theme는 이미 반영해 두었으므로,
// 여기서는 setTheme() 이후의 상태 변경만 문서에 반영한다.
const applyThemeToDocument = (next: Theme): void => {
  document.documentElement.setAttribute('data-theme', next);
};

export const subscribeTheme = (listener: Listener): (() => void) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

export const getTheme = (): Theme => theme;

export const setTheme = (next: Theme): void => {
  if (next === theme) return;
  theme = next;

  try {
    localStorage.setItem(STORAGE_KEY, next);
  } catch {
    // 저장 실패해도 현재 세션의 테마 적용 자체는 계속 진행한다.
  }

  applyThemeToDocument(next);
  listeners.forEach((listener) => listener());
};

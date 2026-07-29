// Microsoft Clarity 커맨드 큐 함수 — 실제 스크립트 로드 전 호출은 q 배열에 쌓였다가
// 스크립트 로드 후 순서대로 처리된다 (Clarity 공식 스니펫과 동일한 동작).
type ClarityFn = {
  (...args: unknown[]): void;
  q?: unknown[];
};

declare global {
  interface Window {
    clarity?: ClarityFn;
  }
}

const CLARITY_SCRIPT_ID = 'ms-clarity-script';

let isInitialized = false;

// 앱 시작 시 한 번만 호출한다.
// - VITE_CLARITY_PROJECT_ID가 없으면(로컬 개발 환경 등) 아무 것도 하지 않고 조용히 종료한다.
// - 이미 초기화된 상태에서 다시 호출해도(StrictMode 이중 실행 등) 스크립트를 다시 삽입하지 않는다.
// - 초기화 실패가 앱 렌더링을 방해하지 않도록 전체를 try/catch로 감싼다.
export function initializeClarity(): void {
  if (isInitialized) return;

  const projectId = import.meta.env.VITE_CLARITY_PROJECT_ID;
  if (!projectId) return;

  // 스크립트 태그가 이미 삽입되어 있다면(HMR 재실행 등) 중복 삽입하지 않는다.
  if (document.getElementById(CLARITY_SCRIPT_ID)) {
    isInitialized = true;
    return;
  }

  try {
    window.clarity =
      window.clarity ||
      function (...args: unknown[]) {
        (window.clarity!.q = window.clarity!.q || []).push(args);
      };

    const script = document.createElement('script');
    script.id = CLARITY_SCRIPT_ID;
    script.async = true;
    script.src = `https://www.clarity.ms/tag/${projectId}`;
    document.head.appendChild(script);

    isInitialized = true;
  } catch (error) {
    // Clarity 초기화 실패가 앱 동작에 영향을 주지 않도록 에러를 삼킨다.
    console.warn('[clarity] 초기화에 실패했습니다.', error);
  }
}

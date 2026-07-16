type Listener = () => void;

// Refresh Token까지 만료/무효화되어 더 이상 인증을 유지할 수 없는 상태를 앱 전역에 알리기 위한 최소 pub-sub.
// fetchWithAuth는 React 컴포넌트가 아니라 일반 모듈이라 훅을 쓸 수 없으므로,
// 이 모듈을 매개로 App 루트에 상시 마운트된 SessionExpiredModal에 이벤트를 전달한다.
let notified = false;
const listeners = new Set<Listener>();

export const subscribeSessionExpired = (listener: Listener): (() => void) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

// 여러 API가 동시에 401을 받아 각자 재발급에 실패하더라도, 안내 모달은 한 번만 뜨도록 플래그로 막는다.
export const notifySessionExpired = (): void => {
  if (notified) return;
  notified = true;
  listeners.forEach((listener) => listener());
};

// 재로그인/토큰 재발급으로 세션이 다시 유효해지면, 다음 만료 때 또 안내할 수 있도록 리셋한다.
export const resetSessionExpiredNotice = (): void => {
  notified = false;
};

// 세션이 이미 만료 안내된 상태인지 확인.
// 자동 저장처럼 백그라운드에서 반복 시도되는 로직이, 만료 후에도 불필요한 요청을 계속 보내지 않도록 사전에 걸러내는 용도.
export const isSessionExpired = (): boolean => notified;

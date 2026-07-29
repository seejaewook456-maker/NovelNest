import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { initializeSentry } from './lib/sentry'
import { initializeClarity } from './lib/clarity'

// 세 분석/모니터링 도구 모두 React 렌더링 전에, 한 번만 초기화한다.
// 각 initialize 함수는 내부적으로 실패를 삼키므로(try/catch), 한 도구의 초기화 실패가
// 다른 도구나 앱 렌더링 자체를 막지 않는다. Sentry는 렌더링 오류를 잡는
// AppErrorBoundary(App.tsx)보다 먼저 준비되어 있어야 하므로 가장 먼저 호출한다.
// Clarity는 GA4(router/index.tsx)와 달리 첫 페이지 뷰를 직접 전송할 필요가 없어
// import 순서 문제가 없으므로, 앱 진입점에서 한 번만 호출하면 된다.
initializeSentry()
initializeClarity()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)

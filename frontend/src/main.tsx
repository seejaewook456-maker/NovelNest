import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { initializeClarity } from './lib/clarity'

// Clarity는 GA4(router/index.tsx)와 달리 첫 페이지 뷰를 직접 전송할 필요가 없어
// import 순서 문제가 없으므로, 앱 진입점에서 한 번만 호출하면 된다.
initializeClarity()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)

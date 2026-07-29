import type { ReactNode } from 'react';
import * as Sentry from '@sentry/react';
import Button from './Button';

// 렌더링 오류 시 흰 화면 대신 보여줄 안내 화면 — 기존 auth-wrapper/auth-card 스타일을 그대로
// 재사용해 Warm Brown + Cream 디자인을 유지한다. 사용자 입력이나 작품 내용은 다루지 않는다.
function ErrorFallback() {
  return (
    <div className="auth-wrapper">
      <div className="auth-card" style={{ textAlign: 'center' }}>
        <p className="auth-brand">노벨네스트</p>
        <h2 className="auth-title" style={{ marginBottom: 16 }}>
          문제가 발생했습니다
        </h2>
        <p style={{ color: 'var(--color-text-secondary)', marginBottom: 28, lineHeight: 1.6 }}>
          화면을 불러오는 중 문제가 발생했습니다.
          <br />
          잠시 후 다시 시도해주세요.
        </p>
        {/* resetError() 대신 실제 새로고침을 사용해, 같은 오류가 즉시 다시 렌더링되는 것을 막는다 */}
        <Button variant="primary" fullWidth onClick={() => window.location.reload()}>
          새로고침
        </Button>
      </div>
    </div>
  );
}

interface AppErrorBoundaryProps {
  children: ReactNode;
}

// 앱 최상위(App.tsx)에서 라우터 전체를 감싸는 Error Boundary.
// Sentry.ErrorBoundary가 렌더링 오류를 캡처해 전송하며(Sentry 비활성 상태에서도 안전한 no-op),
// fallback UI만 이 프로젝트 디자인에 맞게 커스터마이즈했다.
export default function AppErrorBoundary({ children }: AppErrorBoundaryProps) {
  return <Sentry.ErrorBoundary fallback={ErrorFallback}>{children}</Sentry.ErrorBoundary>;
}

import { useEffect, useState } from 'react';
import { subscribeSessionExpired } from '../state/sessionExpired';
import Button from './Button';

// Refresh Token까지 만료/무효화되어 세션을 유지할 수 없을 때 앱 전역에서 뜨는 안내 모달.
// fetchWithAuth(어느 페이지에서 호출되든)가 notifySessionExpired()를 호출하면 여기서 감지해 노출한다.
// 확인 시에만 로그인 페이지로 이동시켜, 사용자가 왜 이동하는지 먼저 인지하게 한다.
export default function SessionExpiredModal() {
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    return subscribeSessionExpired(() => setIsOpen(true));
  }, []);

  if (!isOpen) return null;

  const goToLogin = () => {
    setIsOpen(false);
    // 토큰은 이미 fetchWithAuth에서 정리된 상태 — 전체 새로고침으로 이동해 남은 화면 상태도 초기화한다.
    window.location.href = '/login';
  };

  return (
    <div className="modal-overlay" onClick={goToLogin}>
      <div className="modal-box" onClick={(e) => e.stopPropagation()}>
        <h2 className="modal-title">로그인이 만료되었습니다.</h2>
        <p className="modal-description">보안을 위해 다시 로그인해주세요.</p>
        <div className="modal-actions">
          <Button variant="primary" onClick={goToLogin}>
            확인
          </Button>
        </div>
      </div>
    </div>
  );
}

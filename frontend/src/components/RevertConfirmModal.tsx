import Button from './Button';

interface RevertConfirmModalProps {
  isOpen: boolean;
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
}

// 회차 수정 중 "취소"를 눌렀을 때, 곧바로 되돌리지 않고 의사를 먼저 확인하는 모달.
// "예"를 눌러야만 수정 이전 버전으로 되돌리고, "아니오"를 누르면 수정 화면을 그대로 유지한다.
export default function RevertConfirmModal({ isOpen, onConfirm, onCancel, loading = false }: RevertConfirmModalProps) {
  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-box">
        <h2 className="modal-title">수정 이전 버전으로 돌아가시겠습니까?</h2>
        <p className="modal-description">
          지금까지 수정한 내용은 저장되지 않고, 수정을 시작하기 전 내용으로 되돌아갑니다.
        </p>
        <div className="modal-actions">
          <Button variant="secondary" onClick={onCancel} disabled={loading}>아니오</Button>
          <Button variant="primary" onClick={onConfirm} disabled={loading}>
            {loading ? '되돌리는 중...' : '예'}
          </Button>
        </div>
      </div>
    </div>
  );
}

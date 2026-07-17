import Button from './Button';

interface DraftRecoveryModalProps {
  isOpen: boolean;
  onRecover: () => void;
  onDiscard: () => void;
}

// 신규 회차 작성 중 LocalStorage에 남아있던 초안을 발견했을 때, 복구 여부를 먼저 확정받는 모달.
// 결정 전까지는 작성 폼을 노출하지 않아 초안이 실수로 덮어써지는 것을 막는다.
export default function DraftRecoveryModal({ isOpen, onRecover, onDiscard }: DraftRecoveryModalProps) {
  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-box">
        <h2 className="modal-title">저장되지 않은 작성 내용이 있습니다</h2>
        <p className="modal-description">
          이전에 작성하던 회차 제목/본문이 이 브라우저에 임시 저장되어 있습니다.
          복구하시겠습니까?
        </p>
        <div className="modal-actions">
          <Button variant="secondary" onClick={onDiscard}>새로 작성</Button>
          <Button variant="primary" onClick={onRecover}>복구</Button>
        </div>
      </div>
    </div>
  );
}

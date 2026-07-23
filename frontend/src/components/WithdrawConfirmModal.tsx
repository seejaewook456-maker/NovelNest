import { useState, useEffect, useRef, type ChangeEvent, type KeyboardEvent } from 'react';
import Button from './Button';

const CONFIRM_PHRASE = '회원 탈퇴';

interface WithdrawConfirmModalProps {
  isOpen: boolean;
  onConfirm: () => void;
  onCancel: () => void;
  isLoading?: boolean;
  error?: string;
}

export default function WithdrawConfirmModal({
  isOpen,
  onConfirm,
  onCancel,
  isLoading = false,
  error = '',
}: WithdrawConfirmModalProps) {
  const [confirmText, setConfirmText] = useState('');
  const modalRef = useRef<HTMLDivElement>(null);

  // 모달이 닫힐 때(취소든 탈퇴 성공이든) 입력값을 초기화한다.
  useEffect(() => {
    if (!isOpen) setConfirmText('');
  }, [isOpen]);

  if (!isOpen) return null;

  const canWithdraw = confirmText === CONFIRM_PHRASE && !isLoading;

  // 요청 진행 중에는 모달 외부 클릭으로 닫히지 않도록 제한한다.
  const handleOverlayClick = () => {
    if (isLoading) return;
    onCancel();
  };

  const handleCancelClick = () => {
    if (isLoading) return;
    onCancel();
  };

  // Escape로 닫기(요청 중에는 제한) + Tab 포커스가 모달 밖으로 나가지 않도록 순환시킨다.
  const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    if (e.key === 'Escape') {
      if (!isLoading) onCancel();
      return;
    }

    if (e.key !== 'Tab' || !modalRef.current) return;

    const focusables = modalRef.current.querySelectorAll<HTMLElement>(
      'button:not(:disabled), input:not(:disabled)'
    );
    if (focusables.length === 0) return;

    const first = focusables[0];
    const last = focusables[focusables.length - 1];

    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault();
      last.focus();
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault();
      first.focus();
    }
  };

  return (
    <div className="modal-overlay" onClick={handleOverlayClick} onKeyDown={handleKeyDown}>
      <div
        ref={modalRef}
        className="modal-box withdraw-modal-box"
        role="dialog"
        aria-modal="true"
        aria-labelledby="withdraw-modal-title"
        aria-describedby="withdraw-modal-desc"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="modal-title" id="withdraw-modal-title">
          정말 회원 탈퇴하시겠어요?
        </h2>
        <p className="modal-description" id="withdraw-modal-desc">
          회원 탈퇴 후에는 더 이상 로그인하거나 작품에 접근할 수 없습니다.
          현재 저장된 작품과 회차는 즉시 삭제되지 않지만, 탈퇴한 계정으로 다시 이용할 수 없습니다.
        </p>
        <p className="modal-description modal-warning-extra">
          현재는 탈퇴한 이메일과 소셜 계정으로 재가입할 수 없습니다.
        </p>

        <div className="modal-confirm-section">
          <p className="modal-confirm-label" id="withdraw-confirm-label">
            계속하려면 아래 입력창에 '{CONFIRM_PHRASE}'를 입력해 주세요.
          </p>
          <p className="modal-confirm-phrase">{CONFIRM_PHRASE}</p>
          <input
            className="modal-confirm-input"
            type="text"
            value={confirmText}
            onChange={(e: ChangeEvent<HTMLInputElement>) => setConfirmText(e.target.value)}
            onKeyDown={(e) => {
              // Enter 입력만으로 실수 탈퇴되지 않도록 — 반드시 '탈퇴하기' 버튼을 눌러야 한다.
              if (e.key === 'Enter') e.preventDefault();
            }}
            placeholder={CONFIRM_PHRASE}
            autoFocus
            autoComplete="off"
            disabled={isLoading}
            aria-describedby="withdraw-confirm-label"
          />
        </div>

        {error && (
          <p className="error-message" role="alert">
            {error}
          </p>
        )}

        <div className="modal-actions withdraw-modal-actions">
          <Button variant="secondary" onClick={handleCancelClick} disabled={isLoading}>
            취소
          </Button>
          <Button variant="danger" onClick={onConfirm} disabled={!canWithdraw}>
            {isLoading ? '탈퇴 처리 중...' : '탈퇴하기'}
          </Button>
        </div>
      </div>
    </div>
  );
}

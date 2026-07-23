import { useState, useEffect, useRef } from 'react';
import type { MouseEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyNovels, deleteNovel } from '../api/novelApi';
import { withdrawUser } from '../api/authApi';
import { ApiError, NetworkError } from '../api/fetchWithAuth';
import { clearTokens, isLoggedIn } from '../utils/token';
import type { Novel } from '../types/novel';
import Button from '../components/Button';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import ConfirmDeleteModal from '../components/ConfirmDeleteModal';
import WithdrawConfirmModal from '../components/WithdrawConfirmModal';

// 탈퇴 요청이 서버에서는 이미 성공했는데 응답을 받지 못해 재시도된 경우, 백엔드가 내려주는 코드.
// 이 경우는 사용자 입장에서 이미 탈퇴가 완료된 상태이므로 에러로 보여주지 않고 성공과 동일하게 마무리한다.
const ALREADY_WITHDRAWN_CODE = 'USER_ALREADY_WITHDRAWN';

export default function NovelListPage() {
  const navigate = useNavigate();
  const [novels, setNovels] = useState<Novel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // 삭제 확인 모달 상태
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  // 회원 탈퇴 모달 상태
  const [isWithdrawModalOpen, setIsWithdrawModalOpen] = useState(false);
  const [isWithdrawing, setIsWithdrawing] = useState(false);
  const [withdrawError, setWithdrawError] = useState('');
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const withdrawRedirectTimerRef = useRef<number | null>(null);

  useEffect(() => {
    getMyNovels()
      .then(setNovels)
      .catch((err) => setError(err instanceof Error ? err.message : '목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  // 언마운트 시 예약된 리다이렉트 타이머가 남아있지 않도록 정리한다.
  useEffect(() => {
    return () => {
      if (withdrawRedirectTimerRef.current !== null) {
        window.clearTimeout(withdrawRedirectTimerRef.current);
      }
    };
  }, []);

  // 삭제 버튼 클릭 → 모달만 열기 (API 호출 없음)
  const handleDeleteClick = (e: MouseEvent, novelId: number) => {
    e.stopPropagation();
    setDeleteError('');
    setDeleteTargetId(novelId);
  };

  // 모달에서 삭제 확정 → 기존 삭제 API 호출
  const handleConfirmDelete = async () => {
    if (deleteTargetId === null) return;
    setDeleteLoading(true);
    setDeleteError('');
    try {
      await deleteNovel(deleteTargetId);
      setNovels((prev) => prev.filter((n) => n.id !== deleteTargetId));
      setDeleteTargetId(null);
    } catch (err) {
      setDeleteError(err instanceof Error ? err.message : '삭제에 실패했습니다.');
    } finally {
      setDeleteLoading(false);
    }
  };

  // 탈퇴 성공(또는 이미 탈퇴되어 있어 성공과 동일하게 처리해야 하는 경우) 공통 마무리 처리.
  // 순서: 모달 닫기 → 로컬 토큰 제거 → 안내 메시지 → 잠시 후 로그인 페이지로 이동.
  // (서버 탈퇴 API가 이미 Refresh Token을 제거했으므로, 여기서 별도로 로그아웃 API를 호출하지 않는다.)
  const finishWithdrawSuccess = () => {
    setIsWithdrawModalOpen(false);
    clearTokens();
    setToast({ message: '회원 탈퇴가 완료되었습니다.', type: 'success' });
    withdrawRedirectTimerRef.current = window.setTimeout(() => {
      navigate('/login', { replace: true });
    }, 1200);
  };

  // 회원 탈퇴 버튼 클릭 → 즉시 API 호출 없이 확인 모달만 연다.
  const handleWithdrawClick = () => {
    setWithdrawError('');
    setIsWithdrawModalOpen(true);
  };

  // 모달의 "탈퇴하기" 확정 → 유효한 Access Token으로 먼저 API를 호출하고,
  // 성공을 확인한 뒤에만 로컬 토큰/인증 상태를 정리한다 (반대 순서로 하면 인증 실패로 이어짐).
  const handleConfirmWithdraw = async () => {
    if (isWithdrawing) return; // 중복 클릭/중복 요청 방지
    setIsWithdrawing(true);
    setWithdrawError('');
    try {
      await withdrawUser();
      finishWithdrawSuccess();
    } catch (err) {
      // Access Token 재발급까지 실패한 경우, fetchWithAuth가 이미 토큰을 정리하고
      // 전역 SessionExpiredModal에 안내를 위임한 상태다 — 별도 에러 문구 없이 모달만 닫는다.
      if (!isLoggedIn()) {
        setIsWithdrawModalOpen(false);
        return;
      }

      // 응답을 받지 못한 채 재시도했을 가능성 — 이미 탈퇴된 상태이므로
      // 기술적인 중복 오류를 노출하는 대신 성공과 동일하게 마무리한다.
      if (err instanceof ApiError && err.code === ALREADY_WITHDRAWN_CODE) {
        finishWithdrawSuccess();
        return;
      }

      if (err instanceof NetworkError) {
        setWithdrawError('서버에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.');
      } else if (err instanceof ApiError) {
        setWithdrawError(err.message);
      } else {
        setWithdrawError('회원 탈퇴 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
      }
    } finally {
      setIsWithdrawing(false);
    }
  };

  const handleCancelWithdraw = () => {
    if (isWithdrawing) return;
    setIsWithdrawModalOpen(false);
    setWithdrawError('');
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <p className="error-message">{error}</p>;

  return (
    <div className="novel-list-page">
      <div className="novel-list-body">
        <PageHeader
          title="내 작품 목록"
          action={
            novels.length > 0 ? (
              <Button variant="primary" onClick={() => navigate('/novels/new')}>
                + 새 작품
              </Button>
            ) : undefined
          }
        />

        {novels.length === 0 ? (
          <EmptyState
            message="아직 작품이 없습니다."
            action={
              <Button variant="primary" onClick={() => navigate('/novels/new')}>
                첫 번째 작품 만들기
              </Button>
            }
          />
        ) : (
          <div className="novel-grid">
            {novels.map((novel) => (
              <div key={novel.id} className="novel-card" onClick={() => navigate(`/novels/${novel.id}`)}>
                <h3>{novel.title}</h3>
                <span className="genre">{novel.genre}</span>
                {novel.description && <p className="description">{novel.description}</p>}
                <div style={{ marginTop: 14, textAlign: 'right' }}>
                  <Button variant="danger" size="sm" onClick={(e) => handleDeleteClick(e, novel.id)}>
                    삭제
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 작품 관련 기능과 명확히 분리된 하단 영역 — 눈에 띄지 않는 회원 탈퇴 진입점.
          novel-list-page가 flex column이고 novel-list-body가 flex:1이라 작품 수와 무관하게
          항상 화면(main-content) 맨 아래에 붙는다. */}
      <div className="withdraw-section">
        <Button variant="subtle-danger" size="sm" onClick={handleWithdrawClick}>
          회원 탈퇴
        </Button>
      </div>

      <ConfirmDeleteModal
        isOpen={deleteTargetId !== null}
        title="작품을 삭제하시겠습니까?"
        description="이 작업은 되돌릴 수 없습니다.
작품을 삭제하면 해당 작품의 회차, 등장인물, 세계관 설정, AI 분석 결과가 함께 삭제될 수 있습니다."
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeleteTargetId(null)}
        isLoading={deleteLoading}
        error={deleteError}
      />

      <WithdrawConfirmModal
        isOpen={isWithdrawModalOpen}
        onConfirm={handleConfirmWithdraw}
        onCancel={handleCancelWithdraw}
        isLoading={isWithdrawing}
        error={withdrawError}
      />

      {toast && <div className={`toast toast-${toast.type}`}>{toast.message}</div>}
    </div>
  );
}

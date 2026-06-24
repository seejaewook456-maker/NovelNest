import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { createCharacter, updateCharacter } from '../api/characterApi';
import type { CharacterCandidate } from '../types/characterExtraction';

interface ReviewState {
  candidates: CharacterCandidate[];
  novelId: number;
  episodeTitle: string;
}

export default function CharacterReviewPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as ReviewState | null;

  // state가 없으면(직접 URL 접근) 홈으로 보냄
  if (!state) {
    navigate('/novels', { replace: true });
    return null;
  }

  const { candidates, novelId, episodeTitle } = state;

  const [currentIndex, setCurrentIndex] = useState(0);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);
  const [savedCount, setSavedCount] = useState(0);

  const current = candidates[currentIndex];
  const total = candidates.length;

  // 다음 후보로 이동하거나 완료 처리
  const goNext = () => {
    if (currentIndex + 1 >= total) {
      setDone(true);
    } else {
      setCurrentIndex((prev) => prev + 1);
      setError('');
    }
  };

  // 건너뛰기 — 저장 없이 다음으로
  const handleSkip = () => goNext();

  // 신규 인물 등록
  const handleCreate = async () => {
    setSaving(true);
    setError('');
    try {
      await createCharacter(novelId, {
        name: current.name,
        role: current.role ?? undefined,
        age: current.age ?? undefined,
        personality: current.personality ?? undefined,
        speechStyle: current.speechStyle ?? undefined,
        description: current.description ?? undefined,
      });
      setSavedCount((prev) => prev + 1);
      goNext();
    } catch (err) {
      setError(err instanceof Error ? err.message : '등록 실패');
    } finally {
      setSaving(false);
    }
  };

  // 기존 인물 정보 업데이트 — newInsights를 기존 값에 병합
  const handleUpdate = async () => {
    if (!current.matchedCharacterId || !current.existingCharacter) return;
    setSaving(true);
    setError('');
    try {
      const existing = current.existingCharacter;
      const insights = current.newInsights;

      // newInsights의 새 항목을 기존 값에 추가 (중복 제거)
      const mergeList = (base: string | null, additions: string[] | undefined): string => {
        const baseItems = base ? base.split(',').map((s) => s.trim()).filter(Boolean) : [];
        const newItems = (additions ?? []).filter((item) => !baseItems.includes(item));
        return [...baseItems, ...newItems].join(', ');
      };

      await updateCharacter(current.matchedCharacterId, {
        name: existing.name,
        role: existing.role ?? undefined,
        age: existing.age ?? undefined,
        personality: mergeList(existing.personality, insights?.personality) || undefined,
        speechStyle: mergeList(existing.speechStyle, insights?.speechStyle) || undefined,
        description: existing.description ?? undefined,
      });
      setSavedCount((prev) => prev + 1);
      goNext();
    } catch (err) {
      setError(err instanceof Error ? err.message : '업데이트 실패');
    } finally {
      setSaving(false);
    }
  };

  // 완료 화면
  if (done) {
    return (
      <div className="review-done">
        <h2>검토 완료!</h2>
        <p className="review-done-msg">
          총 {total}명 중 {savedCount}명을 저장했습니다.
        </p>
        <button className="btn-primary" onClick={() => navigate(-1)}>
          회차로 돌아가기
        </button>
      </div>
    );
  }

  const isExisting = current.isExistingCharacter && current.existingCharacter !== null;

  return (
    <div className="review-page">
      {/* 진행 상태 헤더 */}
      <div className="review-progress">
        <span className="back-link" onClick={() => navigate(-1)}>
          ← 회차로 돌아가기
        </span>
        <div className="review-progress-info">
          <span className="review-episode-title">{episodeTitle}</span>
          <span className="review-step">
            {currentIndex + 1} / {total}
          </span>
        </div>
      </div>

      <h2 className="review-title">
        {isExisting ? '기존 인물 — 새 정보 발견' : '신규 인물 발견'}
      </h2>

      {/* 인물 카드 */}
      <div className={`candidate-card ${isExisting ? 'card-existing' : 'card-new'}`}>
        <div className="candidate-name-row">
          <h3 className="candidate-name">{current.name}</h3>
          {isExisting && <span className="badge-existing">기존 인물</span>}
          {!isExisting && <span className="badge-new">신규 인물</span>}
        </div>

        {/* 신규 인물: AI가 추출한 정보 표시 */}
        {!isExisting && (
          <div className="candidate-fields">
            <FieldRow label="역할" value={current.role} />
            <FieldRow label="나이" value={current.age != null ? `${current.age}세` : null} />
            <FieldRow label="성격" value={current.personality} />
            <FieldRow label="말투" value={current.speechStyle} />
            <FieldRow label="설명" value={current.description} />
            {current.evidence && (
              <div className="evidence-box">
                <span className="evidence-label">근거 장면</span>
                <p className="evidence-text">{current.evidence}</p>
              </div>
            )}
          </div>
        )}

        {/* 기존 인물: 현재 DB 정보 + 새로 발견된 정보 비교 */}
        {isExisting && current.existingCharacter && (
          <div className="existing-compare">
            <div className="compare-section">
              <h4 className="compare-title">현재 등록된 정보</h4>
              <div className="candidate-fields">
                <FieldRow label="역할" value={current.existingCharacter.role} />
                <FieldRow label="나이" value={current.existingCharacter.age != null ? `${current.existingCharacter.age}세` : null} />
                <FieldRow label="성격" value={current.existingCharacter.personality} />
                <FieldRow label="말투" value={current.existingCharacter.speechStyle} />
                <FieldRow label="설명" value={current.existingCharacter.description} />
              </div>
            </div>

            {/* newInsights — 새로 발견된 정보 하이라이트 */}
            {current.newInsights &&
              ((current.newInsights.personality?.length ?? 0) > 0 ||
                (current.newInsights.speechStyle?.length ?? 0) > 0) && (
                <div className="insights-section">
                  <h4 className="insights-title">새로 발견된 정보</h4>
                  {(current.newInsights.personality?.length ?? 0) > 0 && (
                    <div className="insights-group">
                      <span className="insights-label">성격</span>
                      <div className="insights-tags">
                        {current.newInsights.personality!.map((item, i) => (
                          <span key={i} className="insight-tag">{item}</span>
                        ))}
                      </div>
                    </div>
                  )}
                  {(current.newInsights.speechStyle?.length ?? 0) > 0 && (
                    <div className="insights-group">
                      <span className="insights-label">말투</span>
                      <div className="insights-tags">
                        {current.newInsights.speechStyle!.map((item, i) => (
                          <span key={i} className="insight-tag">{item}</span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}

            {current.newInsights &&
              (current.newInsights.personality?.length ?? 0) === 0 &&
              (current.newInsights.speechStyle?.length ?? 0) === 0 && (
                <p className="insights-none">이 회차에서 새로 발견된 정보가 없습니다.</p>
              )}

            {current.evidence && (
              <div className="evidence-box">
                <span className="evidence-label">근거 장면</span>
                <p className="evidence-text">{current.evidence}</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* 오류 메시지 */}
      {error && <p className="error-message">{error}</p>}

      {/* 액션 버튼 */}
      <div className="review-actions">
        {!isExisting ? (
          <button className="btn-primary" onClick={handleCreate} disabled={saving}>
            {saving ? '등록 중...' : '등장인물 등록'}
          </button>
        ) : (
          <button className="btn-primary" onClick={handleUpdate} disabled={saving}>
            {saving ? '업데이트 중...' : '정보 업데이트'}
          </button>
        )}
        <button className="btn-secondary" onClick={handleSkip} disabled={saving}>
          건너뛰기
        </button>
      </div>
    </div>
  );
}

// 단순 필드 한 줄 표시 컴포넌트
function FieldRow({ label, value }: { label: string; value: string | null | undefined }) {
  if (!value) return null;
  return (
    <div className="field-row">
      <span className="field-label">{label}</span>
      <span className="field-value">{value}</span>
    </div>
  );
}

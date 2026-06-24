import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { createCharacter, updateCharacter } from '../api/characterApi';
import { linkCharacterToEpisode } from '../api/episodeCharacterApi';
import type { CharacterCandidate } from '../types/characterExtraction';
import Button from '../components/Button';
import ProgressBar from '../components/ProgressBar';

interface ReviewState {
  candidates: CharacterCandidate[];
  novelId: number;
  episodeId: number;
  episodeTitle: string;
}

export default function CharacterReviewPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as ReviewState | null;

  if (!state) {
    navigate('/novels', { replace: true });
    return null;
  }

  const { candidates, novelId, episodeId, episodeTitle } = state;

  const [currentIndex, setCurrentIndex] = useState(0);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);
  const [savedCount, setSavedCount] = useState(0);

  const current = candidates[currentIndex];
  const total = candidates.length;

  const goNext = () => {
    if (currentIndex + 1 >= total) {
      setDone(true);
    } else {
      setCurrentIndex((prev) => prev + 1);
      setError('');
    }
  };

  const handleSkip = () => goNext();

  const handleCreate = async () => {
    setSaving(true);
    setError('');
    try {
      const created = await createCharacter(novelId, {
        name: current.name,
        role: current.role ?? undefined,
        age: current.age ?? undefined,
        personality: current.personality ?? undefined,
        speechStyle: current.speechStyle ?? undefined,
        description: current.description ?? undefined,
      });
      await linkCharacterToEpisode(episodeId, created.id);
      setSavedCount((prev) => prev + 1);
      goNext();
    } catch (err) {
      setError(err instanceof Error ? err.message : '등록 실패');
    } finally {
      setSaving(false);
    }
  };

  const handleUpdate = async () => {
    if (!current.matchedCharacterId || !current.existingCharacter) return;
    setSaving(true);
    setError('');
    try {
      const existing = current.existingCharacter;
      const insights = current.newInsights;

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
      await linkCharacterToEpisode(episodeId, current.matchedCharacterId);
      setSavedCount((prev) => prev + 1);
      goNext();
    } catch (err) {
      setError(err instanceof Error ? err.message : '업데이트 실패');
    } finally {
      setSaving(false);
    }
  };

  if (done) {
    return (
      <div className="review-done">
        <h2>검토 완료!</h2>
        <p className="review-done-msg">
          총 {total}명 중 {savedCount}명을 저장했습니다.
        </p>
        <Button variant="primary" onClick={() => navigate(-1)}>
          회차로 돌아가기
        </Button>
      </div>
    );
  }

  const isExisting = current.isExistingCharacter && current.existingCharacter !== null;
  const hasNewInsights =
    (current.newInsights?.personality?.length ?? 0) > 0 ||
    (current.newInsights?.speechStyle?.length ?? 0) > 0;

  return (
    <div className="review-page">
      {/* 헤더 */}
      <div className="review-header">
        <span className="back-link" style={{ marginBottom: 0 }} onClick={() => navigate(-1)}>
          ← 회차로 돌아가기
        </span>
        <span className="review-step-badge">{currentIndex + 1} / {total}</span>
      </div>

      {/* 진행률 바 */}
      <ProgressBar current={currentIndex + 1} total={total} />

      <p className="review-episode-title" style={{ marginBottom: 8 }}>{episodeTitle}</p>
      <h2 className="review-section-title">
        {isExisting ? '기존 인물 — 새 정보 발견' : '신규 인물 발견'}
      </h2>

      {/* 인물 카드 */}
      <div className={`candidate-card ${isExisting ? 'card-existing' : 'card-new'}`}>
        <div className="candidate-name-row">
          <h3 className="candidate-name">{current.name}</h3>
          {isExisting
            ? <span className="badge-existing">기존 인물</span>
            : <span className="badge-new">신규 인물</span>
          }
        </div>

        {/* 신규 인물 */}
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

        {/* 기존 인물 */}
        {isExisting && current.existingCharacter && (
          <div className="existing-compare">
            <div className="compare-section">
              <p className="compare-title">현재 등록된 정보</p>
              <div className="candidate-fields">
                <FieldRow label="역할" value={current.existingCharacter.role} />
                <FieldRow label="나이" value={current.existingCharacter.age != null ? `${current.existingCharacter.age}세` : null} />
                <FieldRow label="성격" value={current.existingCharacter.personality} />
                <FieldRow label="말투" value={current.existingCharacter.speechStyle} />
                <FieldRow label="설명" value={current.existingCharacter.description} />
              </div>
            </div>

            {hasNewInsights && (
              <div className="insights-section">
                <p className="insights-title">새로 발견된 정보</p>
                {(current.newInsights!.personality?.length ?? 0) > 0 && (
                  <div className="insights-group">
                    <span className="insights-label">성격</span>
                    <div className="insights-tags">
                      {current.newInsights!.personality!.map((item, i) => (
                        <span key={i} className="insight-tag">★ {item}</span>
                      ))}
                    </div>
                  </div>
                )}
                {(current.newInsights!.speechStyle?.length ?? 0) > 0 && (
                  <div className="insights-group">
                    <span className="insights-label">말투</span>
                    <div className="insights-tags">
                      {current.newInsights!.speechStyle!.map((item, i) => (
                        <span key={i} className="insight-tag">★ {item}</span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}

            {current.newInsights && !hasNewInsights && (
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

      {error && <p className="error-message">{error}</p>}

      <div className="review-actions">
        {!isExisting ? (
          <Button variant="primary" onClick={handleCreate} disabled={saving}>
            {saving ? '등록 중...' : '등장인물 등록'}
          </Button>
        ) : (
          <Button variant="primary" onClick={handleUpdate} disabled={saving}>
            {saving ? '업데이트 중...' : '정보 업데이트'}
          </Button>
        )}
        <Button variant="secondary" onClick={handleSkip} disabled={saving}>
          건너뛰기
        </Button>
      </div>
    </div>
  );
}

function FieldRow({ label, value }: { label: string; value: string | null | undefined }) {
  if (!value) return null;
  return (
    <div className="field-row">
      <span className="field-label">{label}</span>
      <span className="field-value">{value}</span>
    </div>
  );
}

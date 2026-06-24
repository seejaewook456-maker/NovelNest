import { useState, useEffect } from 'react';
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

// 기존 목록에 AI 제안 항목을 병합 (중복 제거)
function mergeList(base: string | null, additions: string[] | undefined): string {
  const baseItems = base ? base.split(',').map((s) => s.trim()).filter(Boolean) : [];
  const newItems = (additions ?? []).filter((item) => !baseItems.includes(item));
  return [...baseItems, ...newItems].join(', ');
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
  const total = candidates.length;

  const [currentIndex, setCurrentIndex] = useState(0);
  const [editedName, setEditedName] = useState(candidates[0].name);
  const [editedRole, setEditedRole] = useState(candidates[0].role ?? '');
  const [editedAge, setEditedAge] = useState(
    candidates[0].age != null ? String(candidates[0].age) : ''
  );
  const [editedPersonality, setEditedPersonality] = useState(candidates[0].personality ?? '');
  const [editedSpeechStyle, setEditedSpeechStyle] = useState(candidates[0].speechStyle ?? '');
  const [editedDescription, setEditedDescription] = useState(candidates[0].description ?? '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);
  const [savedCount, setSavedCount] = useState(0);
  const [updatedCount, setUpdatedCount] = useState(0);
  const [skippedCount, setSkippedCount] = useState(0);

  const current = candidates[currentIndex];
  const isLast = currentIndex + 1 >= total;
  const isExisting = current.isExistingCharacter && current.existingCharacter !== null;
  const hasNewInsights =
    (current.newInsights?.personality?.length ?? 0) > 0 ||
    (current.newInsights?.speechStyle?.length ?? 0) > 0;

  // 인덱스가 바뀌면 편집 필드를 현재 후보 값으로 초기화
  useEffect(() => {
    const c = candidates[currentIndex];
    if (c.isExistingCharacter && c.existingCharacter) {
      // 기존 인물: 등록된 정보 + newInsights 병합 값으로 초기화
      const ex = c.existingCharacter;
      setEditedName(ex.name);
      setEditedRole(ex.role ?? '');
      setEditedAge(ex.age != null ? String(ex.age) : '');
      setEditedPersonality(mergeList(ex.personality, c.newInsights?.personality));
      setEditedSpeechStyle(mergeList(ex.speechStyle, c.newInsights?.speechStyle));
      setEditedDescription(ex.description ?? '');
    } else {
      // 신규 인물: AI 추출 값으로 초기화
      setEditedName(c.name);
      setEditedRole(c.role ?? '');
      setEditedAge(c.age != null ? String(c.age) : '');
      setEditedPersonality(c.personality ?? '');
      setEditedSpeechStyle(c.speechStyle ?? '');
      setEditedDescription(c.description ?? '');
    }
    setError('');
  }, [currentIndex]);

  const goNext = () => {
    if (isLast) {
      setDone(true);
    } else {
      setCurrentIndex((prev) => prev + 1);
    }
  };

  const handleSkip = () => {
    setSkippedCount((prev) => prev + 1);
    goNext();
  };

  const handleCreate = async () => {
    setSaving(true);
    setError('');
    try {
      const created = await createCharacter(novelId, {
        name: editedName,
        role: editedRole || undefined,
        age: editedAge ? Number(editedAge) : undefined,
        personality: editedPersonality || undefined,
        speechStyle: editedSpeechStyle || undefined,
        description: editedDescription || undefined,
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
    if (!current.matchedCharacterId) return;
    setSaving(true);
    setError('');
    try {
      await updateCharacter(current.matchedCharacterId, {
        name: editedName,
        role: editedRole || undefined,
        age: editedAge ? Number(editedAge) : undefined,
        personality: editedPersonality || undefined,
        speechStyle: editedSpeechStyle || undefined,
        description: editedDescription || undefined,
      });
      await linkCharacterToEpisode(episodeId, current.matchedCharacterId);
      setUpdatedCount((prev) => prev + 1);
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
        <p className="review-done-msg">총 {total}명의 인물 후보 검토가 끝났습니다.</p>
        <div className="ws-done-stats">
          <div className="ws-done-stat">
            <span className="ws-done-stat-num">{savedCount}</span>
            <span className="ws-done-stat-label">신규 등록</span>
          </div>
          <div className="ws-done-stat">
            <span className="ws-done-stat-num">{updatedCount}</span>
            <span className="ws-done-stat-label">기존 보강</span>
          </div>
          <div className="ws-done-stat">
            <span className="ws-done-stat-num">{skippedCount}</span>
            <span className="ws-done-stat-label">건너뜀</span>
          </div>
        </div>
        <div className="review-actions" style={{ justifyContent: 'center' }}>
          <Button variant="primary" onClick={() => navigate(`/novels/${novelId}/characters`)}>
            등장인물 목록으로
          </Button>
          <Button variant="secondary" onClick={() => navigate(`/episodes/${episodeId}`)}>
            회차로 돌아가기
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="review-page">
      {/* 헤더 */}
      <div className="review-header">
        <span className="back-link" style={{ marginBottom: 0 }} onClick={() => navigate(-1)}>
          ← 회차로 돌아가기
        </span>
        <span className="review-step-badge">{currentIndex + 1} / {total}</span>
      </div>

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

        {/* 기존 인물 — 현재 등록 정보 + newInsights 표시 */}
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

            {!hasNewInsights && (
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

        {/* 신규 인물 — evidence만 표시 */}
        {!isExisting && current.evidence && (
          <div className="evidence-box">
            <span className="evidence-label">근거 장면</span>
            <p className="evidence-text">{current.evidence}</p>
          </div>
        )}

        {/* 편집 영역 — 신규/기존 모두 공통 */}
        <div className="ws-edit-section">
          <p className="compare-title" style={{ marginBottom: 12 }}>
            {isExisting ? 'AI 제안 내용 (수정 후 적용)' : '저장할 내용 (수정 가능)'}
          </p>
          <div className="form-group">
            <label>이름</label>
            <input
              type="text"
              value={editedName}
              onChange={(e) => setEditedName(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label>역할</label>
            <input
              type="text"
              value={editedRole}
              onChange={(e) => setEditedRole(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label>나이</label>
            <input
              type="number"
              min={0}
              value={editedAge}
              onChange={(e) => setEditedAge(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label>성격</label>
            <textarea
              value={editedPersonality}
              onChange={(e) => setEditedPersonality(e.target.value)}
              rows={3}
            />
          </div>
          <div className="form-group">
            <label>말투</label>
            <input
              type="text"
              value={editedSpeechStyle}
              onChange={(e) => setEditedSpeechStyle(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label>설명</label>
            <textarea
              value={editedDescription}
              onChange={(e) => setEditedDescription(e.target.value)}
              rows={4}
            />
          </div>
        </div>
      </div>

      {error && <p className="error-message">{error}</p>}

      <div className="review-actions">
        {!isExisting ? (
          <Button variant="primary" onClick={handleCreate} disabled={saving || !editedName.trim()}>
            {saving ? '등록 중...' : isLast ? '등록하고 완료' : '등록하고 다음 인물'}
          </Button>
        ) : (
          <Button variant="primary" onClick={handleUpdate} disabled={saving || !editedName.trim()}>
            {saving ? '적용 중...' : isLast ? '보강 적용하고 완료' : '보강 내용 적용'}
          </Button>
        )}
        <Button variant="secondary" onClick={handleSkip} disabled={saving}>
          {isLast ? '건너뛰고 완료' : '건너뛰기'}
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

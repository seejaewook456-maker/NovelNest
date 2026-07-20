import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { createWorldSetting, updateWorldSetting } from '../api/worldSettingApi';
import { linkWorldSettingToEpisode } from '../api/episodeWorldSettingApi';
import type { WorldSettingCandidate } from '../types/worldSettingExtraction';
import type { WorldSettingCategory } from '../types/worldsetting';
import { CATEGORY_LABELS } from '../types/worldsetting';
import Button from '../components/Button';
import ProgressBar from '../components/ProgressBar';

interface ReviewState {
  candidates: WorldSettingCandidate[];
  novelId: number;
  episodeId: number;
  episodeTitle: string;
}

const CATEGORY_OPTIONS: WorldSettingCategory[] = [
  'COUNTRY', 'RACE', 'MAGIC', 'ORGANIZATION', 'PLACE', 'EVENT', 'ITEM', 'RULE', 'ETC',
];

export default function WorldSettingReviewPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as ReviewState | null;
  const candidates = state?.candidates ?? [];
  const total = candidates.length;
  // state가 없거나(새로고침·URL 직접 접근 등) 검토할 후보가 하나도 없는 경우 — 목록으로 돌려보낸다
  const isValid = state !== null && total > 0;

  // 아래 훅들은 isValid 여부와 무관하게 항상 동일한 순서로 호출되어야 하므로,
  // "state 없으면 return" 같은 조기 종료보다 반드시 앞에 와야 한다 (React Hooks 규칙).
  const [currentIndex, setCurrentIndex] = useState(0);
  const [editedCategory, setEditedCategory] = useState<WorldSettingCategory>(candidates[0]?.category ?? 'ETC');
  const [editedTitle, setEditedTitle] = useState(candidates[0]?.title ?? '');
  const [editedContent, setEditedContent] = useState(candidates[0]?.content ?? '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);
  const [savedCount, setSavedCount] = useState(0);
  const [updatedCount, setUpdatedCount] = useState(0);
  const [skippedCount, setSkippedCount] = useState(0);

  const current = candidates[currentIndex];

  // 유효하지 않은 진입(state 없음/후보 0개)의 리다이렉트는 부수효과이므로 effect에서 처리한다
  // (렌더링 도중 navigate를 직접 호출하지 않음)
  useEffect(() => {
    if (!isValid) {
      navigate('/novels', { replace: true });
    }
  }, [isValid, navigate]);

  // 인덱스가 바뀌면 편집 필드를 현재 후보 값으로 초기화
  useEffect(() => {
    if (!current) return;
    setEditedCategory(current.category);
    setEditedTitle(current.title);
    setEditedContent(current.content);
    setError('');
  }, [currentIndex]);

  // 모든 훅 호출이 끝난 뒤에만 조기 반환한다 — isValid가 false인 렌더에서도
  // 위 훅들은 이미 다 호출된 상태라 다음 렌더와 훅 호출 순서가 항상 동일하게 유지된다.
  if (!isValid || !state || !current) {
    return null;
  }

  const { novelId, episodeId, episodeTitle } = state;
  const isLast = currentIndex + 1 >= total;
  const isExisting = current.isExistingSetting && current.existingWorldSetting !== null;
  const hasNewInsights = (current.newInsights?.content?.length ?? 0) > 0;

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
      const created = await createWorldSetting(novelId, {
        category: editedCategory,
        title: editedTitle,
        content: editedContent,
      });
      await linkWorldSettingToEpisode(episodeId, created.id);
      setSavedCount((prev) => prev + 1);
      goNext();
    } catch (err) {
      setError(err instanceof Error ? err.message : '저장 실패');
    } finally {
      setSaving(false);
    }
  };

  const handleUpdate = async () => {
    if (!current.matchedWorldSettingId) return;
    setSaving(true);
    setError('');
    try {
      await updateWorldSetting(current.matchedWorldSettingId, {
        category: editedCategory,
        title: editedTitle,
        content: editedContent,
      });
      await linkWorldSettingToEpisode(episodeId, current.matchedWorldSettingId);
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
        <p className="review-done-msg">총 {total}개 설정 후보 검토가 끝났습니다.</p>
        <div className="ws-done-stats">
          <div className="ws-done-stat">
            <span className="ws-done-stat-num">{savedCount}</span>
            <span className="ws-done-stat-label">신규 저장</span>
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
          <Button variant="primary" onClick={() => navigate(`/novels/${novelId}/world-settings`)}>
            세계관 목록으로
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
        {isExisting ? '기존 설정 — 새 정보 발견' : '신규 설정 발견'}
      </h2>

      {/* 설정 카드 */}
      <div className={`candidate-card ${isExisting ? 'card-existing' : 'card-new'}`}>
        <div className="candidate-name-row">
          <h3 className="candidate-name">{current.title}</h3>
          {isExisting
            ? <span className="badge-existing">기존 설정</span>
            : <span className="badge-new">신규 설정</span>
          }
        </div>

        {/* 기존 설정 보강 — 기존 내용 + newInsights 표시 */}
        {isExisting && current.existingWorldSetting && (
          <div className="existing-compare">
            <div className="compare-section">
              <p className="compare-title">현재 등록된 정보</p>
              <div className="candidate-fields">
                <div className="field-row">
                  <span className="field-label">분류</span>
                  <span className="field-value">{CATEGORY_LABELS[current.existingWorldSetting.category]}</span>
                </div>
                <div className="field-row">
                  <span className="field-label">내용</span>
                  <span className="field-value">{current.existingWorldSetting.content}</span>
                </div>
              </div>
            </div>

            {hasNewInsights && (
              <div className="insights-section">
                <p className="insights-title">새로 발견된 정보</p>
                <div className="insights-tags">
                  {current.newInsights!.content.map((item, i) => (
                    <span key={i} className="insight-tag">★ {item}</span>
                  ))}
                </div>
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

        {/* 신규 설정 — evidence만 표시 */}
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
            <label>분류</label>
            <select
              value={editedCategory}
              onChange={(e) => setEditedCategory(e.target.value as WorldSettingCategory)}
            >
              {CATEGORY_OPTIONS.map((cat) => (
                <option key={cat} value={cat}>{CATEGORY_LABELS[cat]}</option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>제목</label>
            <input
              type="text"
              value={editedTitle}
              onChange={(e) => setEditedTitle(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label>내용</label>
            <textarea
              value={editedContent}
              onChange={(e) => setEditedContent(e.target.value)}
              rows={4}
            />
          </div>
        </div>
      </div>

      {error && <p className="error-message">{error}</p>}

      <div className="review-actions">
        {!isExisting ? (
          <Button variant="primary" onClick={handleCreate} disabled={saving || !editedTitle.trim() || !editedContent.trim()}>
            {saving ? '저장 중...' : isLast ? '저장하고 완료' : '저장하고 다음 설정'}
          </Button>
        ) : (
          <Button variant="primary" onClick={handleUpdate} disabled={saving || !editedTitle.trim() || !editedContent.trim()}>
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

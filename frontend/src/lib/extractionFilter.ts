import type { CharacterCandidate } from '../types/characterExtraction';
import type { WorldSettingCandidate } from '../types/worldSettingExtraction';

// AI 추출 결과 중 "카드로 보여줄 가치가 있는" 후보만 남기는 필터.
// 백엔드에서 이미 변경사항 없는 기존 항목을 제외하지만,
// 최종 렌더링 단계에서도 한 번 더 확인해 빈 카드가 표시되지 않도록 한다.

const hasItems = (list: string[] | null | undefined) =>
  (list ?? []).some((item) => item.trim().length > 0);

// 기존 인물 후보에 실제로 추가·수정할 정보가 있는지
export function hasCharacterChanges(c: CharacterCandidate): boolean {
  const insights = c.newInsights;
  if (!insights) return false;
  return (
    hasItems(insights.personality) ||
    hasItems(insights.speechStyle) ||
    hasItems(insights.description) ||
    (insights.role != null && insights.role.trim().length > 0) ||
    insights.age != null
  );
}

// 신규 인물이거나, 변경사항이 있는 기존 인물만 남긴다
export function filterCharacterCandidates(candidates: CharacterCandidate[]): CharacterCandidate[] {
  return candidates.filter((c) => {
    const isExisting = c.isExistingCharacter && c.existingCharacter !== null;
    return !isExisting || hasCharacterChanges(c);
  });
}

// 기존 설정 후보에 실제로 추가·보완할 정보가 있는지
export function hasWorldSettingChanges(c: WorldSettingCandidate): boolean {
  if (hasItems(c.newInsights?.content)) return true;
  // 서버가 newContent를 기존 내용 뒤에 병합해 content를 다시 계산하므로,
  // content가 기존 내용과 다르면 보강할 내용이 있다는 뜻이다
  return (c.content ?? '').trim() !== (c.existingWorldSetting?.content ?? '').trim();
}

// 신규 설정이거나, 변경사항이 있는 기존 설정만 남긴다
export function filterWorldSettingCandidates(candidates: WorldSettingCandidate[]): WorldSettingCandidate[] {
  return candidates.filter((c) => {
    const isExisting = c.isExistingSetting && c.existingWorldSetting !== null;
    return !isExisting || hasWorldSettingChanges(c);
  });
}

export type ConflictType =
  | 'CHARACTER_CONFLICT'
  | 'PERSONALITY_CONFLICT'
  | 'RELATIONSHIP_CONFLICT'
  | 'WORLD_SETTING_CONFLICT'
  | 'ABILITY_CONFLICT'
  | 'TIMELINE_CONFLICT';

export type ConflictSeverity = 'HIGH' | 'MEDIUM' | 'LOW';

export interface ConflictResult {
  type: ConflictType;
  severity: ConflictSeverity;
  title: string;
  existingInfo: string;
  currentEpisodeInfo: string;
  description: string;
  suggestion: string;
}

export interface ConflictDetectionResult {
  episodeTitle: string;
  conflictCount: number;
  conflicts: ConflictResult[];
}

export const CONFLICT_TYPE_LABELS: Record<ConflictType, string> = {
  CHARACTER_CONFLICT: '인물 정보 충돌',
  PERSONALITY_CONFLICT: '성격/행동 일관성 문제',
  RELATIONSHIP_CONFLICT: '인물 관계 충돌',
  WORLD_SETTING_CONFLICT: '세계관 설정 충돌',
  ABILITY_CONFLICT: '능력/마법 설정 충돌',
  TIMELINE_CONFLICT: '시간선 충돌',
};

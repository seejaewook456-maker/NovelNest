export interface NewInsights {
  personality: string[];
  speechStyle: string[];
  // 외모·능력·배경·관계 등 설명에 보완할 새 정보 (구버전 응답 호환을 위해 optional)
  description?: string[] | null;
  // 기존과 다르게 확인된 역할/나이 — 변경이 없으면 null
  role?: string | null;
  age?: number | null;
}

export interface ExistingCharacterInfo {
  id: number;
  novelId: number;
  name: string;
  role: string | null;
  age: number | null;
  personality: string | null;
  speechStyle: string | null;
  description: string | null;
}

export interface CharacterCandidate {
  name: string;
  role: string | null;
  age: number | null;
  personality: string | null;
  speechStyle: string | null;
  description: string | null;
  evidence: string | null;
  isExistingCharacter: boolean;
  matchedCharacterId: number | null;
  newInsights: NewInsights | null;
  existingCharacter: ExistingCharacterInfo | null;
}

export interface CharacterExtractionResult {
  episodeTitle: string;
  totalCount: number;
  candidates: CharacterCandidate[];
}

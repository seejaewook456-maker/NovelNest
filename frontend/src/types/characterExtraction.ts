export interface NewInsights {
  personality: string[];
  speechStyle: string[];
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

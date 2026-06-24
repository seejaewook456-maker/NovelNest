import type { CharacterExtractionResult } from '../types/characterExtraction';
import { fetchWithAuth } from './fetchWithAuth';

export const extractCharacters = async (episodeId: number): Promise<CharacterExtractionResult> => {
  const json = await fetchWithAuth<CharacterExtractionResult>(
    `/api/episodes/${episodeId}/character-extraction`,
    { method: 'POST' }
  );
  return json.data!;
};

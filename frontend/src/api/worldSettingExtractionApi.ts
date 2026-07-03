import type { WorldSettingExtractionResult } from '../types/worldSettingExtraction';
import { fetchWithAuth } from './fetchWithAuth';

export const extractWorldSettings = async (episodeId: number): Promise<WorldSettingExtractionResult> => {
  const json = await fetchWithAuth<WorldSettingExtractionResult>(
    `/episodes/${episodeId}/world-setting-extraction`,
    { method: 'POST' }
  );
  return json.data!;
};

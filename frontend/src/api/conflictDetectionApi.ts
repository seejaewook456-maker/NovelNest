import type { ConflictDetectionResult } from '../types/conflictDetection';
import { fetchWithAuth } from './fetchWithAuth';

export const detectConflicts = async (episodeId: number): Promise<ConflictDetectionResult> => {
  const json = await fetchWithAuth<ConflictDetectionResult>(
    `/api/episodes/${episodeId}/conflict-detection`,
    { method: 'POST' }
  );
  return json.data!;
};

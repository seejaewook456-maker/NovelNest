import type { EpisodeSummary } from '../types/episodeSummary';
import { fetchWithAuth } from './fetchWithAuth';

export const generateSummary = async (episodeId: number): Promise<EpisodeSummary> => {
  const json = await fetchWithAuth<EpisodeSummary>(`/api/episodes/${episodeId}/summary`, {
    method: 'POST',
  });
  return json.data!;
};

export const getSummary = async (episodeId: number): Promise<EpisodeSummary | null> => {
  try {
    const json = await fetchWithAuth<EpisodeSummary>(`/api/episodes/${episodeId}/summary`);
    return json.data ?? null;
  } catch {
    // 요약이 없는 경우(400) — null 반환
    return null;
  }
};

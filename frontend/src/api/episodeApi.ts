import type { Episode, EpisodeCreateRequest, EpisodeUpdateRequest } from '../types/episode';
import { fetchWithAuth } from './fetchWithAuth';

export const getEpisodes = async (novelId: number): Promise<Episode[]> => {
  const json = await fetchWithAuth<Episode[]>(`/novels/${novelId}/episodes`);
  return json.data!;
};

export const createEpisode = async (novelId: number, body: EpisodeCreateRequest): Promise<Episode> => {
  const json = await fetchWithAuth<Episode>(`/novels/${novelId}/episodes`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
  return json.data!;
};

export const getEpisode = async (episodeId: number): Promise<Episode> => {
  const json = await fetchWithAuth<Episode>(`/episodes/${episodeId}`);
  return json.data!;
};

export const updateEpisode = async (episodeId: number, body: EpisodeUpdateRequest): Promise<Episode> => {
  const json = await fetchWithAuth<Episode>(`/episodes/${episodeId}`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  });
  return json.data!;
};

export const deleteEpisode = async (episodeId: number): Promise<void> => {
  await fetchWithAuth(`/episodes/${episodeId}`, { method: 'DELETE' });
};

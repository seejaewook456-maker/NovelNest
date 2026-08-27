import type { Episode, EpisodeBrief, EpisodeCreateRequest, EpisodeUpdateRequest } from '../types/episode';
import { fetchWithAuth } from './fetchWithAuth';

export const getEpisodes = async (novelId: number): Promise<Episode[]> => {
  const json = await fetchWithAuth<Episode[]>(`/novels/${novelId}/episodes`);
  return json.data!;
};

// "이전 회차" 참고 패널 전용 — 본문 없이 번호+제목만 가져온다(getEpisodes는 전체 본문을 포함해 무겁다).
export const getEpisodeBriefs = async (novelId: number): Promise<EpisodeBrief[]> => {
  const json = await fetchWithAuth<EpisodeBrief[]>(`/novels/${novelId}/episodes/brief`);
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

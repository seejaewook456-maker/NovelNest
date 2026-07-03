import type { WorldSetting } from '../types/worldsetting';
import { fetchWithAuth } from './fetchWithAuth';

export const linkWorldSettingToEpisode = async (episodeId: number, worldSettingId: number): Promise<void> => {
  await fetchWithAuth(`/episodes/${episodeId}/world-settings/${worldSettingId}`, { method: 'POST' });
};

export const getEpisodeWorldSettings = async (episodeId: number): Promise<WorldSetting[]> => {
  const json = await fetchWithAuth<WorldSetting[]>(`/episodes/${episodeId}/world-settings`);
  return json.data!;
};

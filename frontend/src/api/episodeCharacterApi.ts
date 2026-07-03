import type { Character } from '../types/character';
import { fetchWithAuth } from './fetchWithAuth';

export const linkCharacterToEpisode = async (episodeId: number, characterId: number): Promise<void> => {
  await fetchWithAuth(`/episodes/${episodeId}/characters/${characterId}`, { method: 'POST' });
};

export const getEpisodeCharacters = async (episodeId: number): Promise<Character[]> => {
  const json = await fetchWithAuth<Character[]>(`/episodes/${episodeId}/characters`);
  return json.data!;
};

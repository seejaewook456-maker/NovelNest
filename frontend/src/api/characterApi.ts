import type { Character, CharacterCreateRequest, CharacterUpdateRequest } from '../types/character';
import { fetchWithAuth } from './fetchWithAuth';

export const getCharacters = async (novelId: number): Promise<Character[]> => {
  const json = await fetchWithAuth<Character[]>(`/novels/${novelId}/characters`);
  return json.data!;
};

export const createCharacter = async (novelId: number, body: CharacterCreateRequest): Promise<Character> => {
  const json = await fetchWithAuth<Character>(`/novels/${novelId}/characters`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
  return json.data!;
};

export const updateCharacter = async (characterId: number, body: CharacterUpdateRequest): Promise<Character> => {
  const json = await fetchWithAuth<Character>(`/characters/${characterId}`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  });
  return json.data!;
};

export const toggleCharacterFavorite = async (characterId: number, isFavorite: boolean): Promise<Character> => {
  const json = await fetchWithAuth<Character>(`/characters/${characterId}/favorite`, {
    method: 'PATCH',
    body: JSON.stringify({ isFavorite }),
  });
  return json.data!;
};

export const deleteCharacter = async (characterId: number): Promise<void> => {
  await fetchWithAuth(`/characters/${characterId}`, { method: 'DELETE' });
};

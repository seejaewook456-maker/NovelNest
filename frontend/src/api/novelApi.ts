import type { Novel, NovelCreateRequest } from '../types/novel';
import { fetchWithAuth } from './fetchWithAuth';

export const getMyNovels = async (): Promise<Novel[]> => {
  const json = await fetchWithAuth<Novel[]>('/novels');
  return json.data!;
};

export const getNovel = async (novelId: number): Promise<Novel> => {
  const json = await fetchWithAuth<Novel>(`/novels/${novelId}`);
  return json.data!;
};

export const createNovel = async (body: NovelCreateRequest): Promise<Novel> => {
  const json = await fetchWithAuth<Novel>('/novels', {
    method: 'POST',
    body: JSON.stringify(body),
  });
  return json.data!;
};

export const deleteNovel = async (novelId: number): Promise<void> => {
  await fetchWithAuth(`/novels/${novelId}`, { method: 'DELETE' });
};

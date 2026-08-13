import type { Memo, MemoCreateRequest, MemoUpdateRequest } from '../types/memo';
import { fetchWithAuth } from './fetchWithAuth';

export const getMemos = async (novelId: number): Promise<Memo[]> => {
  const json = await fetchWithAuth<Memo[]>(`/novels/${novelId}/memos`);
  return json.data!;
};

export const createMemo = async (novelId: number, body: MemoCreateRequest): Promise<Memo> => {
  const json = await fetchWithAuth<Memo>(`/novels/${novelId}/memos`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
  return json.data!;
};

export const getMemo = async (memoId: number): Promise<Memo> => {
  const json = await fetchWithAuth<Memo>(`/memos/${memoId}`);
  return json.data!;
};

export const updateMemo = async (memoId: number, body: MemoUpdateRequest): Promise<Memo> => {
  const json = await fetchWithAuth<Memo>(`/memos/${memoId}`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  });
  return json.data!;
};

export const toggleMemoFavorite = async (memoId: number, isFavorite: boolean): Promise<Memo> => {
  const json = await fetchWithAuth<Memo>(`/memos/${memoId}/favorite`, {
    method: 'PATCH',
    body: JSON.stringify({ isFavorite }),
  });
  return json.data!;
};

export const deleteMemo = async (memoId: number): Promise<void> => {
  await fetchWithAuth(`/memos/${memoId}`, { method: 'DELETE' });
};

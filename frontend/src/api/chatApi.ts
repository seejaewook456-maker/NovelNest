import type { ContextStats } from '../types/chat';
import { fetchWithAuth } from './fetchWithAuth';

export const getContextStats = async (novelId: number): Promise<ContextStats> => {
  const json = await fetchWithAuth<ContextStats>(`/novels/${novelId}/chat/context-stats`);
  return json.data!;
};

export const sendChatMessage = async (novelId: number, message: string): Promise<string> => {
  const json = await fetchWithAuth<{ answer: string }>(`/novels/${novelId}/chat`, {
    method: 'POST',
    body: JSON.stringify({ message }),
  });
  return json.data!.answer;
};

export interface ContextStats {
  totalEpisodeCount: number;
  summaryCount: number;
  characterCount: number;
  worldSettingCount: number;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
}

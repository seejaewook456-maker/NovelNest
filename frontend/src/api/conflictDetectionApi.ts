import type { ConflictDetectionResult } from '../types/conflictDetection';
import { fetchWithAuth } from './fetchWithAuth';

// 저장된 충돌 탐지 결과 조회 — 없으면 null
export const getConflictResult = async (episodeId: number): Promise<ConflictDetectionResult | null> => {
  const json = await fetchWithAuth<ConflictDetectionResult>(
    `/api/episodes/${episodeId}/conflict-detection`
  );
  return json.data ?? null;
};

// AI 충돌 탐지 실행 후 결과 저장
export const detectConflicts = async (episodeId: number): Promise<ConflictDetectionResult> => {
  const json = await fetchWithAuth<ConflictDetectionResult>(
    `/api/episodes/${episodeId}/conflict-detection`,
    { method: 'POST' }
  );
  return json.data!;
};

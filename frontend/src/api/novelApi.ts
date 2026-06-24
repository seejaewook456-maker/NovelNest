import { getToken } from '../utils/token';
import { Novel, NovelCreateRequest } from '../types/novel';

interface ApiResponse<T = undefined> {
  message: string;
  data?: T;
}

// 인증이 필요한 요청에 Authorization 헤더를 자동으로 붙여주는 헬퍼
const authHeaders = (): HeadersInit => ({
  'Content-Type': 'application/json',
  Authorization: `Bearer ${getToken()}`,
});

export const getMyNovels = async (): Promise<Novel[]> => {
  const res = await fetch('/api/novels', {
    headers: authHeaders(),
  });

  const json: ApiResponse<Novel[]> = await res.json();

  if (!res.ok) {
    throw new Error(json.message || '작품 목록 조회에 실패했습니다.');
  }

  return json.data!;
};

export const createNovel = async (body: NovelCreateRequest): Promise<Novel> => {
  const res = await fetch('/api/novels', {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(body),
  });

  const json: ApiResponse<Novel> = await res.json();

  if (!res.ok) {
    throw new Error(json.message || '작품 생성에 실패했습니다.');
  }

  return json.data!;
};

export const deleteNovel = async (novelId: number): Promise<void> => {
  const res = await fetch(`/api/novels/${novelId}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });

  const json: ApiResponse = await res.json();

  if (!res.ok) {
    throw new Error(json.message || '작품 삭제에 실패했습니다.');
  }
};

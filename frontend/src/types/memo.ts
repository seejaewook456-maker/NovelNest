export interface Memo {
  id: number;
  novelId: number;
  title: string;
  content: string;
  isFavorite: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MemoCreateRequest {
  title: string;
  content: string;
}

// PATCH도 전체 교체 — 두 필드 모두 필수
export interface MemoUpdateRequest {
  title: string;
  content: string;
}

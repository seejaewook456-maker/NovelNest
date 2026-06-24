export interface Novel {
  id: number;
  userId: number;
  title: string;
  genre: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface NovelCreateRequest {
  title: string;
  genre: string;
  description?: string;
}

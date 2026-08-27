export interface Episode {
  id: number;
  novelId: number;
  title: string;
  episodeNumber: number;
  content: string;
  createdAt: string;
  updatedAt: string;
}

// "이전 회차" 참고 패널의 목록 조회용 경량 타입 — 본문(content) 없이 번호+제목만 가진다.
// 회차가 많은 작품에서도 목록 응답을 가볍게 유지하기 위해 Episode와 별도로 둔다.
export interface EpisodeBrief {
  id: number;
  episodeNumber: number;
  title: string;
}

export interface EpisodeCreateRequest {
  title: string;
  episodeNumber: number;
  content: string;
}

// PATCH도 전체 교체 — 세 필드 모두 필수
export interface EpisodeUpdateRequest {
  title: string;
  episodeNumber: number;
  content: string;
}

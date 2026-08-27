package org.example.domain.episode.dto;

import lombok.Getter;
import org.example.domain.episode.entity.Episode;

// 회차 작성/수정 화면의 "이전 회차" 참고 패널처럼 목록에서 번호+제목만 필요한 화면을 위한 경량 DTO.
// 본문(content)을 포함하지 않아, 회차가 수백 개에 달하는 작품에서도 목록 조회 응답 크기를 작게 유지한다.
@Getter
public class EpisodeBriefResponseDto {

    private final Long id;
    private final int episodeNumber;
    private final String title;

    private EpisodeBriefResponseDto(Long id, int episodeNumber, String title) {
        this.id = id;
        this.episodeNumber = episodeNumber;
        this.title = title;
    }

    public static EpisodeBriefResponseDto from(Episode episode) {
        return new EpisodeBriefResponseDto(
                episode.getId(),
                episode.getEpisodeNumber(),
                episode.getTitle()
        );
    }
}

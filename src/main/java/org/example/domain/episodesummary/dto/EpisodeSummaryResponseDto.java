package org.example.domain.episodesummary.dto;

import lombok.Getter;
import org.example.domain.episodesummary.entity.EpisodeSummary;

import java.time.LocalDateTime;

@Getter
public class EpisodeSummaryResponseDto {

    private final Long episodeId;
    private final String summary;
    private final LocalDateTime updatedAt;

    private EpisodeSummaryResponseDto(Long episodeId, String summary, LocalDateTime updatedAt) {
        this.episodeId = episodeId;
        this.summary = summary;
        this.updatedAt = updatedAt;
    }

    public static EpisodeSummaryResponseDto from(EpisodeSummary episodeSummary) {
        return new EpisodeSummaryResponseDto(
                episodeSummary.getEpisode().getId(),
                episodeSummary.getSummary(),
                episodeSummary.getUpdatedAt()
        );
    }
}

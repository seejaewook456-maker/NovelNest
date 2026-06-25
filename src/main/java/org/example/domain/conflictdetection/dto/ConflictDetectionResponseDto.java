package org.example.domain.conflictdetection.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ConflictDetectionResponseDto {

    private final String episodeTitle;
    private final int conflictCount;
    private final List<ConflictResultDto> conflicts;
    private final LocalDateTime analyzedAt;

    public ConflictDetectionResponseDto(String episodeTitle, List<ConflictResultDto> conflicts, LocalDateTime analyzedAt) {
        this.episodeTitle = episodeTitle;
        this.conflictCount = conflicts.size();
        this.conflicts = conflicts;
        this.analyzedAt = analyzedAt;
    }
}

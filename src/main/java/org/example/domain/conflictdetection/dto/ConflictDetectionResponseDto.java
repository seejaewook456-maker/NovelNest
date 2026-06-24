package org.example.domain.conflictdetection.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ConflictDetectionResponseDto {

    private final String episodeTitle;
    private final int conflictCount;
    private final List<ConflictResultDto> conflicts;

    public ConflictDetectionResponseDto(String episodeTitle, List<ConflictResultDto> conflicts) {
        this.episodeTitle = episodeTitle;
        this.conflictCount = conflicts.size();
        this.conflicts = conflicts;
    }
}

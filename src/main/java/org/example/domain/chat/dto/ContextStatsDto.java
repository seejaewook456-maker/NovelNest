package org.example.domain.chat.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ContextStatsDto {

    private final long totalEpisodeCount;
    private final long summaryCount;
    private final long characterCount;
    private final long worldSettingCount;
}

package org.example.domain.episodeworldsetting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.domain.episodeworldsetting.service.EpisodeWorldSettingService;
import org.example.domain.worldsetting.dto.WorldSettingResponseDto;
import org.example.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "회차-세계관 연결", description = "AI 추출로 저장된 회차별 세계관 설정 연결 관리")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class EpisodeWorldSettingController {

    private final EpisodeWorldSettingService episodeWorldSettingService;

    @Operation(
            summary = "회차-세계관 연결 생성",
            description = "AI 추출로 저장한 세계관 설정을 특정 회차에 연결합니다. 이미 연결된 경우 무시됩니다."
    )
    @PostMapping("/api/episodes/{episodeId}/world-settings/{worldSettingId}")
    public ResponseEntity<ApiResponse> linkWorldSetting(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long episodeId,
            @PathVariable Long worldSettingId) {
        episodeWorldSettingService.linkWorldSetting(userDetails.getUsername(), episodeId, worldSettingId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("회차-세계관 연결 성공"));
    }

    @Operation(
            summary = "회차별 추출 세계관 설정 목록 조회",
            description = "해당 회차에서 AI 추출 후 저장된 세계관 설정 목록을 반환합니다."
    )
    @GetMapping("/api/episodes/{episodeId}/world-settings")
    public ResponseEntity<ApiResponse> getWorldSettingsByEpisode(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long episodeId) {
        List<WorldSettingResponseDto> response = episodeWorldSettingService.getWorldSettingsByEpisode(
                userDetails.getUsername(), episodeId);
        return ResponseEntity.ok(ApiResponse.of("회차별 세계관 설정 조회 성공", response));
    }
}

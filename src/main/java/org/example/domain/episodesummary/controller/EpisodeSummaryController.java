package org.example.domain.episodesummary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.domain.episodesummary.dto.EpisodeSummaryResponseDto;
import org.example.domain.episodesummary.service.EpisodeSummaryService;
import org.example.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회차 요약", description = "AI를 활용한 회차 요약 생성 및 조회")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class EpisodeSummaryController {

    private final EpisodeSummaryService episodeSummaryService;

    @Operation(
            summary = "회차 요약 생성",
            description = "AI가 회차 본문을 분석하여 3~5문장 요약을 생성합니다. 이미 요약이 존재하면 새로 덮어씁니다."
    )
    @PostMapping("/api/episodes/{episodeId}/summary")
    public ResponseEntity<ApiResponse> generateSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long episodeId) {
        EpisodeSummaryResponseDto response = episodeSummaryService.generateSummary(
                userDetails.getUsername(), episodeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("회차 요약 생성 성공", response));
    }

    @Operation(
            summary = "회차 요약 조회",
            description = "저장된 회차 요약을 조회합니다. 아직 요약이 생성되지 않은 경우 400을 반환합니다."
    )
    @GetMapping("/api/episodes/{episodeId}/summary")
    public ResponseEntity<ApiResponse> getSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long episodeId) {
        EpisodeSummaryResponseDto response = episodeSummaryService.getSummary(
                userDetails.getUsername(), episodeId);
        return ResponseEntity.ok(ApiResponse.of("회차 요약 조회 성공", response));
    }
}

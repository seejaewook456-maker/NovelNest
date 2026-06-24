package org.example.domain.conflictdetection.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.domain.conflictdetection.dto.ConflictDetectionResponseDto;
import org.example.domain.conflictdetection.service.ConflictDetectionService;
import org.example.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "설정 충돌 탐지", description = "회차 본문을 기존 설정과 비교하여 충돌 가능성을 분석합니다. DB 저장 없이 결과만 반환합니다.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class ConflictDetectionController {

    private final ConflictDetectionService conflictDetectionService;

    @Operation(
            summary = "설정 충돌 탐지",
            description = "현재 회차 본문을 등장인물 정보, 세계관 설정, 이전 회차 요약과 비교하여 충돌 가능성을 반환합니다. " +
                          "AI는 충돌 가능성만 제안하며 자동 수정은 하지 않습니다. DB 저장 없음."
    )
    @PostMapping("/api/episodes/{episodeId}/conflict-detection")
    public ResponseEntity<ApiResponse> detectConflicts(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long episodeId) {

        ConflictDetectionResponseDto result =
                conflictDetectionService.detectConflicts(userDetails.getUsername(), episodeId);

        return ResponseEntity.ok(ApiResponse.of("충돌 탐지 완료", result));
    }
}

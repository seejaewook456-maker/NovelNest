package org.example.domain.aiusage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.domain.aiusage.dto.AiDailyUsageResponseDto;
import org.example.domain.aiusage.service.AiUsageService;
import org.example.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 사용량", description = "로그인한 사용자의 오늘(Asia/Seoul 기준) AI 기능 사용량/잔여 횟수 조회")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class AiUsageController {

    private final AiUsageService aiUsageService;

    @Operation(
            summary = "오늘의 AI 사용량 조회",
            description = "회차 요약/설정 충돌 감지/등장인물 추출/세계관 추출/AI 챗봇 각각의 오늘 사용·잔여·제한 횟수를 " +
                    "반환합니다(기능별 제한만 적용, 전체 합계 제한은 없음). 이 조회 자체는 사용 횟수에 포함되지 않습니다."
    )
    @GetMapping("/api/ai/usage/daily")
    public ResponseEntity<ApiResponse> getDailyUsage(@AuthenticationPrincipal UserDetails userDetails) {
        AiDailyUsageResponseDto response = aiUsageService.getDailyUsage(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.of("AI 사용량 조회 성공", response));
    }
}

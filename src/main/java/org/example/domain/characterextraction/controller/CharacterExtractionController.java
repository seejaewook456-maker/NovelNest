package org.example.domain.characterextraction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.domain.characterextraction.dto.CharacterExtractionResponseDto;
import org.example.domain.characterextraction.service.CharacterExtractionService;
import org.example.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "등장인물 AI 추출", description = "회차 본문을 AI가 분석하여 등장인물 후보를 생성합니다. DB 저장 없이 후보만 반환합니다.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class CharacterExtractionController {

    private final CharacterExtractionService characterExtractionService;

    @Operation(
            summary = "등장인물 AI 추출",
            description = "회차 본문을 분석하여 등장인물 후보 목록을 반환합니다. " +
                          "기존 등장인물과 비교해 신규/기존 인물을 구분하고, 기존 인물은 새로 발견된 정보(newInsights)를 함께 반환합니다. " +
                          "이 API는 후보를 생성할 뿐이며, 실제 저장은 사용자 검토 후 Character CRUD API를 통해 수행합니다."
    )
    @PostMapping("/api/episodes/{episodeId}/character-extraction")
    public ResponseEntity<ApiResponse> extractCharacters(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long episodeId) {

        CharacterExtractionResponseDto result =
                characterExtractionService.extractCharacters(userDetails.getUsername(), episodeId);

        return ResponseEntity.ok(ApiResponse.of("등장인물 추출 완료", result));
    }
}

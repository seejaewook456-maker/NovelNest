package org.example.domain.episodecharacter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.domain.character.dto.CharacterResponseDto;
import org.example.domain.episodecharacter.service.EpisodeCharacterService;
import org.example.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "회차-등장인물 연결", description = "AI 추출로 저장된 회차별 등장인물 연결 관리")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class EpisodeCharacterController {

    private final EpisodeCharacterService episodeCharacterService;

    @Operation(
            summary = "회차-인물 연결 생성",
            description = "AI 추출로 저장한 인물을 특정 회차에 연결합니다. 이미 연결된 경우 무시됩니다."
    )
    @PostMapping("/api/episodes/{episodeId}/characters/{characterId}")
    public ResponseEntity<ApiResponse> linkCharacter(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long episodeId,
            @PathVariable Long characterId) {
        episodeCharacterService.linkCharacter(userDetails.getUsername(), episodeId, characterId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("회차-등장인물 연결 성공"));
    }

    @Operation(
            summary = "회차별 추출 인물 목록 조회",
            description = "해당 회차에서 AI 추출 후 저장된 등장인물 목록을 반환합니다."
    )
    @GetMapping("/api/episodes/{episodeId}/characters")
    public ResponseEntity<ApiResponse> getCharactersByEpisode(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long episodeId) {
        List<CharacterResponseDto> response = episodeCharacterService.getCharactersByEpisode(
                userDetails.getUsername(), episodeId);
        return ResponseEntity.ok(ApiResponse.of("회차별 등장인물 조회 성공", response));
    }
}

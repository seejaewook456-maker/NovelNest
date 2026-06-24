package org.example.domain.episode.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.domain.episode.dto.EpisodeCreateRequestDto;
import org.example.domain.episode.dto.EpisodeResponseDto;
import org.example.domain.episode.dto.EpisodeUpdateRequestDto;
import org.example.domain.episode.service.EpisodeService;
import org.example.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "회차", description = "회차(Episode) 생성 / 조회 / 수정 / 삭제")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class EpisodeController {

    private final EpisodeService episodeService;

    @Operation(summary = "회차 생성", description = "작품에 새로운 회차를 추가합니다. 같은 작품 내 회차 번호는 중복될 수 없습니다.")
    @PostMapping("/api/novels/{novelId}/episodes")
    public ResponseEntity<ApiResponse> createEpisode(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId,
            @Valid @RequestBody EpisodeCreateRequestDto dto) {
        EpisodeResponseDto response = episodeService.createEpisode(userDetails.getUsername(), novelId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("회차 생성 성공", response));
    }

    @Operation(summary = "회차 목록 조회", description = "작품의 전체 회차 목록을 회차 번호 오름차순으로 반환합니다.")
    @GetMapping("/api/novels/{novelId}/episodes")
    public ResponseEntity<ApiResponse> getEpisodes(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId) {
        List<EpisodeResponseDto> response = episodeService.getEpisodes(userDetails.getUsername(), novelId);
        return ResponseEntity.ok(ApiResponse.of("회차 목록 조회 성공", response));
    }

    @Operation(summary = "회차 상세 조회", description = "회차 ID로 상세 내용을 조회합니다. 본인 작품의 회차만 조회 가능합니다.")
    @GetMapping("/api/episodes/{episodeId}")
    public ResponseEntity<ApiResponse> getEpisode(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long episodeId) {
        EpisodeResponseDto response = episodeService.getEpisode(userDetails.getUsername(), episodeId);
        return ResponseEntity.ok(ApiResponse.of("회차 상세 조회 성공", response));
    }

    @Operation(summary = "회차 수정", description = "회차 정보를 수정합니다. 본인 작품의 회차만 수정 가능합니다.")
    @PatchMapping("/api/episodes/{episodeId}")
    public ResponseEntity<ApiResponse> updateEpisode(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long episodeId,
            @Valid @RequestBody EpisodeUpdateRequestDto dto) {
        EpisodeResponseDto response = episodeService.updateEpisode(userDetails.getUsername(), episodeId, dto);
        return ResponseEntity.ok(ApiResponse.of("회차 수정 성공", response));
    }

    @Operation(summary = "회차 삭제", description = "회차를 삭제합니다. 본인 작품의 회차만 삭제 가능합니다.")
    @DeleteMapping("/api/episodes/{episodeId}")
    public ResponseEntity<ApiResponse> deleteEpisode(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long episodeId) {
        episodeService.deleteEpisode(userDetails.getUsername(), episodeId);
        return ResponseEntity.ok(ApiResponse.of("회차 삭제 성공"));
    }
}

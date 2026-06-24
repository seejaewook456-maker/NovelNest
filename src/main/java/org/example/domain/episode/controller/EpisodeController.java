package org.example.domain.episode.controller;

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

@RestController
@RequiredArgsConstructor
public class EpisodeController {

    private final EpisodeService episodeService;

    @PostMapping("/api/novels/{novelId}/episodes")
    public ResponseEntity<ApiResponse> createEpisode(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId,
            @Valid @RequestBody EpisodeCreateRequestDto dto) {
        EpisodeResponseDto response = episodeService.createEpisode(userDetails.getUsername(), novelId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("회차 생성 성공", response));
    }

    @GetMapping("/api/novels/{novelId}/episodes")
    public ResponseEntity<ApiResponse> getEpisodes(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId) {
        List<EpisodeResponseDto> response = episodeService.getEpisodes(userDetails.getUsername(), novelId);
        return ResponseEntity.ok(ApiResponse.of("회차 목록 조회 성공", response));
    }

    @GetMapping("/api/episodes/{episodeId}")
    public ResponseEntity<ApiResponse> getEpisode(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long episodeId) {
        EpisodeResponseDto response = episodeService.getEpisode(userDetails.getUsername(), episodeId);
        return ResponseEntity.ok(ApiResponse.of("회차 상세 조회 성공", response));
    }

    @PatchMapping("/api/episodes/{episodeId}")
    public ResponseEntity<ApiResponse> updateEpisode(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long episodeId,
            @Valid @RequestBody EpisodeUpdateRequestDto dto) {
        EpisodeResponseDto response = episodeService.updateEpisode(userDetails.getUsername(), episodeId, dto);
        return ResponseEntity.ok(ApiResponse.of("회차 수정 성공", response));
    }

    @DeleteMapping("/api/episodes/{episodeId}")
    public ResponseEntity<ApiResponse> deleteEpisode(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long episodeId) {
        episodeService.deleteEpisode(userDetails.getUsername(), episodeId);
        return ResponseEntity.ok(ApiResponse.of("회차 삭제 성공"));
    }
}

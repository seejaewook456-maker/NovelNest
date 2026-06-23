package org.example.domain.novel.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.domain.novel.dto.NovelCreateRequestDto;
import org.example.domain.novel.dto.NovelResponseDto;
import org.example.domain.novel.dto.NovelUpdateRequestDto;
import org.example.domain.novel.service.NovelService;
import org.example.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/novels")
@RequiredArgsConstructor
public class NovelController {

    private final NovelService novelService;

    @PostMapping
    public ResponseEntity<ApiResponse> createNovel(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NovelCreateRequestDto dto) {
        NovelResponseDto response = novelService.createNovel(userDetails.getUsername(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("작품 생성 성공", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getMyNovels(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<NovelResponseDto> response = novelService.getMyNovels(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.of("내 작품 목록 조회 성공", response));
    }

    @GetMapping("/{novelId}")
    public ResponseEntity<ApiResponse> getNovel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId) {
        NovelResponseDto response = novelService.getNovel(userDetails.getUsername(), novelId);
        return ResponseEntity.ok(ApiResponse.of("작품 상세 조회 성공", response));
    }

    @PutMapping("/{novelId}")
    public ResponseEntity<ApiResponse> updateNovel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId,
            @Valid @RequestBody NovelUpdateRequestDto dto) {
        NovelResponseDto response = novelService.updateNovel(userDetails.getUsername(), novelId, dto);
        return ResponseEntity.ok(ApiResponse.of("작품 수정 성공", response));
    }

    @DeleteMapping("/{novelId}")
    public ResponseEntity<ApiResponse> deleteNovel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId) {
        novelService.deleteNovel(userDetails.getUsername(), novelId);
        return ResponseEntity.ok(ApiResponse.of("작품 삭제 성공"));
    }
}

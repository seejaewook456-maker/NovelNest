package org.example.domain.novel.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "작품", description = "작품(Novel) 생성 / 조회 / 수정 / 삭제")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/novels")
@RequiredArgsConstructor
public class NovelController {

    private final NovelService novelService;

    @Operation(summary = "작품 생성", description = "새로운 작품을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse> createNovel(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NovelCreateRequestDto dto) {
        NovelResponseDto response = novelService.createNovel(userDetails.getUsername(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("작품 생성 성공", response));
    }

    @Operation(summary = "내 작품 목록 조회", description = "로그인한 사용자가 소유한 작품 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse> getMyNovels(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<NovelResponseDto> response = novelService.getMyNovels(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.of("내 작품 목록 조회 성공", response));
    }

    @Operation(summary = "작품 상세 조회", description = "작품 ID로 상세 정보를 조회합니다. 본인 작품만 조회 가능합니다.")
    @GetMapping("/{novelId}")
    public ResponseEntity<ApiResponse> getNovel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId) {
        NovelResponseDto response = novelService.getNovel(userDetails.getUsername(), novelId);
        return ResponseEntity.ok(ApiResponse.of("작품 상세 조회 성공", response));
    }

    @Operation(summary = "작품 수정", description = "작품 정보를 수정합니다. 본인 작품만 수정 가능합니다.")
    @PutMapping("/{novelId}")
    public ResponseEntity<ApiResponse> updateNovel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId,
            @Valid @RequestBody NovelUpdateRequestDto dto) {
        NovelResponseDto response = novelService.updateNovel(userDetails.getUsername(), novelId, dto);
        return ResponseEntity.ok(ApiResponse.of("작품 수정 성공", response));
    }

    @Operation(summary = "작품 삭제", description = "작품을 삭제합니다. 본인 작품만 삭제 가능합니다.")
    @DeleteMapping("/{novelId}")
    public ResponseEntity<ApiResponse> deleteNovel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId) {
        novelService.deleteNovel(userDetails.getUsername(), novelId);
        return ResponseEntity.ok(ApiResponse.of("작품 삭제 성공"));
    }
}

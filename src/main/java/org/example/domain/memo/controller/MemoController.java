package org.example.domain.memo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.domain.memo.dto.MemoCreateRequestDto;
import org.example.domain.memo.dto.MemoFavoriteRequestDto;
import org.example.domain.memo.dto.MemoResponseDto;
import org.example.domain.memo.dto.MemoUpdateRequestDto;
import org.example.domain.memo.service.MemoService;
import org.example.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "메모", description = "메모(Memo) 생성 / 조회 / 수정 / 삭제")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class MemoController {

    private final MemoService memoService;

    @Operation(summary = "메모 생성", description = "작품에 새로운 메모를 추가합니다.")
    @PostMapping("/api/novels/{novelId}/memos")
    public ResponseEntity<ApiResponse> createMemo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId,
            @Valid @RequestBody MemoCreateRequestDto dto) {
        MemoResponseDto response = memoService.createMemo(userDetails.getUsername(), novelId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("메모 생성 성공", response));
    }

    @Operation(summary = "메모 목록 조회", description = "작품의 전체 메모 목록을 최근 수정순으로 반환합니다.")
    @GetMapping("/api/novels/{novelId}/memos")
    public ResponseEntity<ApiResponse> getMemos(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId) {
        List<MemoResponseDto> response = memoService.getMemos(userDetails.getUsername(), novelId);
        return ResponseEntity.ok(ApiResponse.of("메모 목록 조회 성공", response));
    }

    @Operation(summary = "메모 상세 조회", description = "메모 ID로 상세 내용을 조회합니다. 본인 작품의 메모만 조회 가능합니다.")
    @GetMapping("/api/memos/{memoId}")
    public ResponseEntity<ApiResponse> getMemo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long memoId) {
        MemoResponseDto response = memoService.getMemo(userDetails.getUsername(), memoId);
        return ResponseEntity.ok(ApiResponse.of("메모 상세 조회 성공", response));
    }

    @Operation(summary = "메모 수정", description = "메모 내용을 수정합니다. 본인 작품의 메모만 수정 가능합니다.")
    @PatchMapping("/api/memos/{memoId}")
    public ResponseEntity<ApiResponse> updateMemo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long memoId,
            @Valid @RequestBody MemoUpdateRequestDto dto) {
        MemoResponseDto response = memoService.updateMemo(userDetails.getUsername(), memoId, dto);
        return ResponseEntity.ok(ApiResponse.of("메모 수정 성공", response));
    }

    @Operation(summary = "메모 즐겨찾기 설정", description = "메모의 즐겨찾기 상태를 변경합니다.")
    @PatchMapping("/api/memos/{memoId}/favorite")
    public ResponseEntity<ApiResponse> toggleFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long memoId,
            @Valid @RequestBody MemoFavoriteRequestDto dto) {
        MemoResponseDto response = memoService.toggleFavorite(userDetails.getUsername(), memoId, dto);
        return ResponseEntity.ok(ApiResponse.of("즐겨찾기 상태 변경 성공", response));
    }

    @Operation(summary = "메모 삭제", description = "메모를 삭제합니다. 본인 작품의 메모만 삭제 가능합니다.")
    @DeleteMapping("/api/memos/{memoId}")
    public ResponseEntity<ApiResponse> deleteMemo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long memoId) {
        memoService.deleteMemo(userDetails.getUsername(), memoId);
        return ResponseEntity.ok(ApiResponse.of("메모 삭제 성공"));
    }
}

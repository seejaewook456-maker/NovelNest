package org.example.domain.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.domain.chat.dto.ChatRequestDto;
import org.example.domain.chat.dto.ChatResponseDto;
import org.example.domain.chat.dto.ContextStatsDto;
import org.example.domain.chat.service.ChatService;
import org.example.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 작품의 AI 데이터 현황 조회 — 페이지 진입 시 즉시 로드
    @GetMapping("/api/novels/{novelId}/chat/context-stats")
    public ResponseEntity<ApiResponse> getContextStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId) {
        ContextStatsDto stats = chatService.getContextStats(userDetails.getUsername(), novelId);
        return ResponseEntity.ok(ApiResponse.of("컨텍스트 통계 조회 성공", stats));
    }

    // AI 글쓰기 비서에게 질문 전송 — 작품 컨텍스트 기반 단발성 답변
    @PostMapping("/api/novels/{novelId}/chat")
    public ResponseEntity<ApiResponse> chat(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId,
            @Valid @RequestBody ChatRequestDto request) {
        ChatResponseDto response = chatService.chat(userDetails.getUsername(), novelId, request.getMessage());
        return ResponseEntity.ok(ApiResponse.of("AI 답변 생성 완료", response));
    }
}

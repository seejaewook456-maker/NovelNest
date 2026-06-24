package org.example.domain.worldsetting.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.domain.worldsetting.dto.WorldSettingCreateRequestDto;
import org.example.domain.worldsetting.dto.WorldSettingResponseDto;
import org.example.domain.worldsetting.dto.WorldSettingUpdateRequestDto;
import org.example.domain.worldsetting.service.WorldSettingService;
import org.example.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WorldSettingController {

    private final WorldSettingService worldSettingService;

    @PostMapping("/api/novels/{novelId}/world-settings")
    public ResponseEntity<ApiResponse> createWorldSetting(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId,
            @Valid @RequestBody WorldSettingCreateRequestDto dto) {
        WorldSettingResponseDto response = worldSettingService.createWorldSetting(userDetails.getUsername(), novelId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("세계관 설정 생성 성공", response));
    }

    @GetMapping("/api/novels/{novelId}/world-settings")
    public ResponseEntity<ApiResponse> getWorldSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId) {
        List<WorldSettingResponseDto> response = worldSettingService.getWorldSettings(userDetails.getUsername(), novelId);
        return ResponseEntity.ok(ApiResponse.of("세계관 설정 목록 조회 성공", response));
    }

    @GetMapping("/api/world-settings/{worldSettingId}")
    public ResponseEntity<ApiResponse> getWorldSetting(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long worldSettingId) {
        WorldSettingResponseDto response = worldSettingService.getWorldSetting(userDetails.getUsername(), worldSettingId);
        return ResponseEntity.ok(ApiResponse.of("세계관 설정 상세 조회 성공", response));
    }

    @PatchMapping("/api/world-settings/{worldSettingId}")
    public ResponseEntity<ApiResponse> updateWorldSetting(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long worldSettingId,
            @Valid @RequestBody WorldSettingUpdateRequestDto dto) {
        WorldSettingResponseDto response = worldSettingService.updateWorldSetting(userDetails.getUsername(), worldSettingId, dto);
        return ResponseEntity.ok(ApiResponse.of("세계관 설정 수정 성공", response));
    }

    @DeleteMapping("/api/world-settings/{worldSettingId}")
    public ResponseEntity<ApiResponse> deleteWorldSetting(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long worldSettingId) {
        worldSettingService.deleteWorldSetting(userDetails.getUsername(), worldSettingId);
        return ResponseEntity.ok(ApiResponse.of("세계관 설정 삭제 성공"));
    }
}

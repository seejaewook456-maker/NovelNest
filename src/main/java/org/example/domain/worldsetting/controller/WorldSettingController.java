package org.example.domain.worldsetting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "세계관", description = "세계관 설정(WorldSetting) 생성 / 조회 / 수정 / 삭제")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class WorldSettingController {

    private final WorldSettingService worldSettingService;

    @Operation(summary = "세계관 설정 생성", description = "작품에 새로운 세계관 설정을 추가합니다. category는 COUNTRY / RACE / MAGIC / ORGANIZATION / PLACE / EVENT / ITEM / RULE / ETC 중 하나입니다.")
    @PostMapping("/api/novels/{novelId}/world-settings")
    public ResponseEntity<ApiResponse> createWorldSetting(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId,
            @Valid @RequestBody WorldSettingCreateRequestDto dto) {
        WorldSettingResponseDto response = worldSettingService.createWorldSetting(userDetails.getUsername(), novelId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("세계관 설정 생성 성공", response));
    }

    @Operation(summary = "세계관 설정 목록 조회", description = "작품의 전체 세계관 설정을 카테고리 → 제목 오름차순으로 반환합니다.")
    @GetMapping("/api/novels/{novelId}/world-settings")
    public ResponseEntity<ApiResponse> getWorldSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId) {
        List<WorldSettingResponseDto> response = worldSettingService.getWorldSettings(userDetails.getUsername(), novelId);
        return ResponseEntity.ok(ApiResponse.of("세계관 설정 목록 조회 성공", response));
    }

    @Operation(summary = "세계관 설정 상세 조회", description = "세계관 설정 ID로 상세 정보를 조회합니다. 본인 작품의 설정만 조회 가능합니다.")
    @GetMapping("/api/world-settings/{worldSettingId}")
    public ResponseEntity<ApiResponse> getWorldSetting(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long worldSettingId) {
        WorldSettingResponseDto response = worldSettingService.getWorldSetting(userDetails.getUsername(), worldSettingId);
        return ResponseEntity.ok(ApiResponse.of("세계관 설정 상세 조회 성공", response));
    }

    @Operation(summary = "세계관 설정 수정", description = "세계관 설정을 수정합니다. 본인 작품의 설정만 수정 가능합니다.")
    @PatchMapping("/api/world-settings/{worldSettingId}")
    public ResponseEntity<ApiResponse> updateWorldSetting(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long worldSettingId,
            @Valid @RequestBody WorldSettingUpdateRequestDto dto) {
        WorldSettingResponseDto response = worldSettingService.updateWorldSetting(userDetails.getUsername(), worldSettingId, dto);
        return ResponseEntity.ok(ApiResponse.of("세계관 설정 수정 성공", response));
    }

    @Operation(summary = "세계관 설정 삭제", description = "세계관 설정을 삭제합니다. 본인 작품의 설정만 삭제 가능합니다.")
    @DeleteMapping("/api/world-settings/{worldSettingId}")
    public ResponseEntity<ApiResponse> deleteWorldSetting(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long worldSettingId) {
        worldSettingService.deleteWorldSetting(userDetails.getUsername(), worldSettingId);
        return ResponseEntity.ok(ApiResponse.of("세계관 설정 삭제 성공"));
    }
}

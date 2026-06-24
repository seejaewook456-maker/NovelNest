package org.example.domain.character.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.domain.character.dto.CharacterCreateRequestDto;
import org.example.domain.character.dto.CharacterResponseDto;
import org.example.domain.character.dto.CharacterUpdateRequestDto;
import org.example.domain.character.service.CharacterService;
import org.example.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "등장인물", description = "등장인물(Character) 생성 / 조회 / 수정 / 삭제")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @Operation(summary = "등장인물 생성", description = "작품에 새로운 등장인물을 추가합니다. 이름만 필수이며 나머지는 선택 입력입니다.")
    @PostMapping("/api/novels/{novelId}/characters")
    public ResponseEntity<ApiResponse> createCharacter(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId,
            @Valid @RequestBody CharacterCreateRequestDto dto) {
        CharacterResponseDto response = characterService.createCharacter(userDetails.getUsername(), novelId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("등장인물 생성 성공", response));
    }

    @Operation(summary = "등장인물 목록 조회", description = "작품의 등장인물 목록을 이름 오름차순으로 반환합니다.")
    @GetMapping("/api/novels/{novelId}/characters")
    public ResponseEntity<ApiResponse> getCharacters(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId) {
        List<CharacterResponseDto> response = characterService.getCharacters(userDetails.getUsername(), novelId);
        return ResponseEntity.ok(ApiResponse.of("등장인물 목록 조회 성공", response));
    }

    @Operation(summary = "등장인물 상세 조회", description = "등장인물 ID로 상세 정보를 조회합니다. 본인 작품의 등장인물만 조회 가능합니다.")
    @GetMapping("/api/characters/{characterId}")
    public ResponseEntity<ApiResponse> getCharacter(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long characterId) {
        CharacterResponseDto response = characterService.getCharacter(userDetails.getUsername(), characterId);
        return ResponseEntity.ok(ApiResponse.of("등장인물 상세 조회 성공", response));
    }

    @Operation(summary = "등장인물 수정", description = "등장인물 정보를 수정합니다. 본인 작품의 등장인물만 수정 가능합니다.")
    @PatchMapping("/api/characters/{characterId}")
    public ResponseEntity<ApiResponse> updateCharacter(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long characterId,
            @Valid @RequestBody CharacterUpdateRequestDto dto) {
        CharacterResponseDto response = characterService.updateCharacter(userDetails.getUsername(), characterId, dto);
        return ResponseEntity.ok(ApiResponse.of("등장인물 수정 성공", response));
    }

    @Operation(summary = "등장인물 삭제", description = "등장인물을 삭제합니다. 본인 작품의 등장인물만 삭제 가능합니다.")
    @DeleteMapping("/api/characters/{characterId}")
    public ResponseEntity<ApiResponse> deleteCharacter(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long characterId) {
        characterService.deleteCharacter(userDetails.getUsername(), characterId);
        return ResponseEntity.ok(ApiResponse.of("등장인물 삭제 성공"));
    }
}

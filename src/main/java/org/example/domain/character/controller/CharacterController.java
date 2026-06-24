package org.example.domain.character.controller;

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

@RestController
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @PostMapping("/api/novels/{novelId}/characters")
    public ResponseEntity<ApiResponse> createCharacter(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId,
            @Valid @RequestBody CharacterCreateRequestDto dto) {
        CharacterResponseDto response = characterService.createCharacter(userDetails.getUsername(), novelId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("등장인물 생성 성공", response));
    }

    @GetMapping("/api/novels/{novelId}/characters")
    public ResponseEntity<ApiResponse> getCharacters(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long novelId) {
        List<CharacterResponseDto> response = characterService.getCharacters(userDetails.getUsername(), novelId);
        return ResponseEntity.ok(ApiResponse.of("등장인물 목록 조회 성공", response));
    }

    @GetMapping("/api/characters/{characterId}")
    public ResponseEntity<ApiResponse> getCharacter(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long characterId) {
        CharacterResponseDto response = characterService.getCharacter(userDetails.getUsername(), characterId);
        return ResponseEntity.ok(ApiResponse.of("등장인물 상세 조회 성공", response));
    }

    @PatchMapping("/api/characters/{characterId}")
    public ResponseEntity<ApiResponse> updateCharacter(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long characterId,
            @Valid @RequestBody CharacterUpdateRequestDto dto) {
        CharacterResponseDto response = characterService.updateCharacter(userDetails.getUsername(), characterId, dto);
        return ResponseEntity.ok(ApiResponse.of("등장인물 수정 성공", response));
    }

    @DeleteMapping("/api/characters/{characterId}")
    public ResponseEntity<ApiResponse> deleteCharacter(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long characterId) {
        characterService.deleteCharacter(userDetails.getUsername(), characterId);
        return ResponseEntity.ok(ApiResponse.of("등장인물 삭제 성공"));
    }
}

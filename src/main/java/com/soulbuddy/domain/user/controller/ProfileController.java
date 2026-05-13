package com.soulbuddy.domain.user.controller;

import com.soulbuddy.domain.user.dto.*;
import com.soulbuddy.domain.user.service.ProfileService;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Profile", description = "온보딩 및 프로필 관리 API")
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(
            summary = "온보딩 제출",
            description = "최초 1회 사용자의 정보를 입력받아 프로필을 생성하고 개인화 지침을 구축합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "온보딩 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 누락 또는 유효성 실패 (VALID_001)")
    })
    @PostMapping("/api/onboarding")
    public ResponseEntity<ApiResponse<OnboardingResponse>> onboard(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal,
            @Valid @RequestBody OnboardingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.onboard(Long.parseLong(principal), request)));
    }

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 온보딩 정보 및 프로필을 조회합니다.")
    @GetMapping("/api/profiles/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.getProfile(Long.parseLong(principal))));
    }

    @Operation(
            summary = "내 프로필 수정",
            description = "프로필 정보를 수정합니다. 수정 시 AI 개인화 지침(Instruction)이 자동으로 갱신됩니다."
    )
    @PutMapping("/api/profiles/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.updateProfile(Long.parseLong(principal), request)));
    }
}
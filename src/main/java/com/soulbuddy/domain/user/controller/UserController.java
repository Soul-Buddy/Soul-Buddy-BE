package com.soulbuddy.domain.user.controller;

import com.soulbuddy.domain.user.dto.UserMeResponse;
import com.soulbuddy.domain.user.dto.UserSettingsResponse;
import com.soulbuddy.domain.user.dto.UserSettingsUpdateRequest;
import com.soulbuddy.domain.user.service.UserService;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 정보 및 설정 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 기본 정보(이메일, 닉네임, 온보딩 완료 여부 등)를 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> getMe(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getMe(Long.parseLong(principal))));
    }

    @Operation(summary = "알림 설정 조회", description = "안전 리소스 동의 여부 등 사용자 알림 설정을 조회합니다.")
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> getSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getSettings(Long.parseLong(principal))));
    }

    @Operation(summary = "알림 설정 변경", description = "안전 리소스 동의 여부 등 알림 설정을 변경합니다.")
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> updateSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal,
            @RequestBody UserSettingsUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateSettings(Long.parseLong(principal), request)));
    }
}

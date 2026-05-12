package com.soulbuddy.be.domain.user.controller;

import com.soulbuddy.be.domain.user.dto.UserMeResponse;
import com.soulbuddy.be.domain.user.entity.User;
import com.soulbuddy.be.domain.user.service.ProfileService;
import com.soulbuddy.be.domain.user.service.UserService;
import com.soulbuddy.be.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 정보 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProfileService profileService;

    @Operation(
            summary = "내 정보 조회",
            description = "Access Token을 이용해 현재 로그인된 사용자의 기본 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserMeResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (토큰 오류)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> getMe(Authentication authentication) {
        // 보통 authentication.getName()은 사용자의 식별값(ID)을 반환하도록 SecurityConfig에서 설정됨
        Long userId = Long.parseLong(authentication.getName());
        User user = userService.getUser(userId);

        UserMeResponse response = UserMeResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileCompleted(profileService.hasProfile(userId))
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
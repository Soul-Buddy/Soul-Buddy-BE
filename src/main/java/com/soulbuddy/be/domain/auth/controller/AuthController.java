package com.soulbuddy.be.domain.auth.controller;

import com.soulbuddy.be.domain.auth.dto.TokenRefreshRequest;
import com.soulbuddy.be.domain.auth.dto.TokenRefreshResponse;
import com.soulbuddy.be.global.auth.JwtTokenProvider;
import com.soulbuddy.be.global.exception.BusinessException;
import com.soulbuddy.be.global.response.ApiResponse;
import com.soulbuddy.be.global.response.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새 Access Token을 발급합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @Valid @RequestBody TokenRefreshRequest request) {
        if (!jwtTokenProvider.validate(request.getRefreshToken())) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }

        Long userId = jwtTokenProvider.getUserId(request.getRefreshToken());
        String newAccessToken = jwtTokenProvider.createAccessToken(userId);

        return ResponseEntity.ok(ApiResponse.success(
                TokenRefreshResponse.builder()
                        .accessToken(newAccessToken)
                        .build()
        ));
    }
}

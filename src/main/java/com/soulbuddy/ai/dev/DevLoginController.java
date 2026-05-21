package com.soulbuddy.ai.dev;

import com.soulbuddy.global.auth.JwtTokenProvider;
import com.soulbuddy.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 단독 테스트용 JWT 발급 엔드포인트.
 *
 * - Google OAuth 흐름을 우회하여 userId 만으로 access token 을 발급한다.
 * - SecurityConfig 의 /api/dev/** permitAll 매칭을 그대로 사용한다.
 * - @Profile("local") 이라 운영 빌드(prod)에서는 빈 등록되지 않는다.
 *   (application.yml 의 spring.profiles.active=local 일 때만 활성)
 */
@Slf4j
@RestController
@RequestMapping("/api/dev/login")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Profile("local")
public class DevLoginController {

    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping
    public ApiResponse<DevLoginResponse> login(@RequestBody DevLoginRequest request) {
        Long userId = request.userId() == null ? 1L : request.userId();
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        log.info("[dev-login] userId={} 의 access token 발급 (local profile only)", userId);
        return ApiResponse.success(new DevLoginResponse(accessToken, userId));
    }

    public record DevLoginRequest(Long userId) {}

    public record DevLoginResponse(String accessToken, Long userId) {}
}

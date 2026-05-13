package com.soulbuddy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
@Schema(description = "온보딩 제출 응답")
public record OnboardingResponse(
        @Schema(description = "생성된 프로필 ID", example = "1")
        Long profileId,

        @Schema(description = "온보딩 완료 일시", example = "2026-05-04T10:05:00Z")
        LocalDateTime onboardingCompletedAt
) {
    // 필요 시 정적 팩토리 메서드 추가
    public static OnboardingResponse of(Long profileId, LocalDateTime completedAt) {
        return OnboardingResponse.builder()
                .profileId(profileId)
                .onboardingCompletedAt(completedAt)
                .build();
    }
}
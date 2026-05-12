package com.soulbuddy.be.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "내 정보 조회 응답")
public class UserMeResponse {

    @Schema(description = "사용자 고유 ID", example = "101")
    private Long userId;

    @Schema(description = "사용자 닉네임", example = "솔버디123")
    private String nickname;

    @Schema(description = "사용자 이메일", example = "user@soulbuddy.com")
    private String email;

    @Schema(description = "프로필 초기 설정 완료 여부", example = "true")
    private boolean profileCompleted;
}
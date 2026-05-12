package com.soulbuddy.be.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Schema(description = "사용자 프로필 수정 요청")
public class UserProfileUpdateRequest {

    @Schema(description = "수정할 닉네임 (필수)", example = "새로운닉네임")
    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    @Schema(description = "성별", example = "MALE")
    private String gender;

    @Schema(description = "직업", example = "직장인")
    private String job;

    @Schema(description = "연령대", example = "30대")
    private String ageGroup;

    @Schema(description = "데일리 체크인 알림 활성화 여부", example = "true")
    private boolean dailyCheckInAlarm;

    @Schema(description = "응원 메시지 알림 활성화 여부", example = "true")
    private boolean cheerMessageAlarm;
}
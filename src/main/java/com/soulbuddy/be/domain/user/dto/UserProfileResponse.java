package com.soulbuddy.be.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "사용자 프로필 상세 조회 응답")
public class UserProfileResponse {

    @Schema(description = "닉네임", example = "솔버디123")
    private String nickname;

    @Schema(description = "성별 (MALE, FEMALE, UNKNOWN)", example = "FEMALE")
    private String gender;

    @Schema(description = "직업", example = "대학생")
    private String job;

    @Schema(description = "연령대", example = "20대")
    private String ageGroup;

    @Schema(description = "프로필 이미지 URL", example = "https://cdn.soulbuddy.com/profiles/1.png")
    private String profileImageUrl;

    @Schema(description = "데일리 체크인 알림 설정 여부", example = "true")
    private boolean dailyCheckInAlarm;

    @Schema(description = "응원 메시지 알림 설정 여부", example = "false")
    private boolean cheerMessageAlarm;

    @Schema(description = "총 활동 내역(히스토리) 횟수", example = "42")
    private int historyCount;
}
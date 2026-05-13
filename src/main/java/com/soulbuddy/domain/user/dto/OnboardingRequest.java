package com.soulbuddy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Schema(description = "온보딩 제출 요청")
public record OnboardingRequest(
        @Schema(description = "사용자 닉네임", example = "준영")
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname,

        @Schema(description = "나이", example = "26")
        @Min(1) @Max(120)
        int age,

        @Schema(description = "성별 (MALE, FEMALE)", example = "MALE")
        @NotBlank(message = "성별을 선택해주세요.")
        String gender,

        @Schema(description = "직업 (미입력 가능)", example = "컴퓨터공학과 학생")
        String occupation,

        @Schema(description = "이용 목적", example = "정서적 공감")
        @NotBlank(message = "이용 목적을 입력해주세요.")
        String usageIntent,

        @Schema(description = "취미 리스트", example = "[\"독서\", \"코딩\"]")
        List<String> hobbies,

        @Schema(description = "선호 말투 (FRIEND, COUNSELOR)", example = "FRIEND")
        @NotBlank(message = "선호하는 말투를 선택해주세요.")
        String preferredTone,

        @Schema(description = "좋아하는 것", example = "[\"산책\", \"고양이\"]")
        List<String> likedThings,

        @Schema(description = "싫어하는 것", example = "[\"소음\", \"당근\"]")
        List<String> dislikedThings
) {}
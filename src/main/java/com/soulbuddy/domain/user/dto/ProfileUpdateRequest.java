package com.soulbuddy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "내 프로필 수정 요청")
public record ProfileUpdateRequest(
        @Schema(description = "사용자 닉네임", example = "준영")
        String nickname,
        @Schema(description = "나이", example = "26")
        int age,
        @Schema(description = "성별", example = "MALE")
        String gender,
        @Schema(description = "직업", example = "컴퓨터공학과 학생")
        String occupation,
        @Schema(description = "이용 목적", example = "정서적 공감")
        String usageIntent,
        @Schema(description = "취미 리스트", example = "[\"독서\"]")
        List<String> hobbies,
        @Schema(description = "선호 말투", example = "FRIEND")
        String preferredTone,
        @Schema(description = "좋아하는 것", example = "[\"커피\"]")
        List<String> likedThings,
        @Schema(description = "싫어하는 것", example = "[\"소음\"]")
        List<String> dislikedThings
) {}
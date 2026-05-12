package com.soulbuddy.be.domain.safety.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

public class SelfAssessmentDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "자가설문 제출 요청 DTO")
    public static class Request {

        @NotNull(message = "사용자 ID는 필수입니다")
        @Schema(description = "사용자 ID", example = "123", required = true)
        private Long userId;

        @NotBlank(message = "세션 ID는 필수입니다")
        @Schema(description = "세션 ID", example = "SESSION-001", required = true)
        private String sessionId;

        @NotNull(message = "설문 응답은 필수입니다")
        @Min(value = 0, message = "점수는 0 이상이어야 합니다")
        @Max(value = 100, message = "점수는 100 이하여야 합니다")
        @Schema(description = "설문 종합 점수 (0-100)", example = "45", required = true,
                minimum = "0", maximum = "100")
        private Integer totalScore;

        @Schema(description = "자살 생각 점수 (0-10)", example = "7",
                minimum = "0", maximum = "10")
        private Integer suicidalThoughtScore;

        @Schema(description = "자해 의도 점수 (0-10)", example = "5",
                minimum = "0", maximum = "10")
        private Integer selfHarmIntentScore;

        @Schema(description = "절망감 점수 (0-10)", example = "6",
                minimum = "0", maximum = "10")
        private Integer hopelessnessScore;

        @Schema(description = "설문 응답 내용 (JSON 형식)", example = "{\"question1\":\"yes\",\"question2\":\"no\"}")
        private String responseData;

        @Schema(description = "위험도 판정 결과", example = "HIGH",
                allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
        private String riskLevel;

        @Schema(description = "설문자 메모 (선택사항)", example = "최근 우울증 악화 추세")
        private String notes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "자가설문 응답 DTO")
    public static class Response {

        @Schema(description = "설문 ID", example = "1")
        private Long id;

        @Schema(description = "사용자 ID", example = "123")
        private Long userId;

        @Schema(description = "세션 ID", example = "SESSION-001")
        private String sessionId;

        @Schema(description = "설문 종합 점수", example = "45")
        private Integer totalScore;

        @Schema(description = "자살 생각 점수", example = "7")
        private Integer suicidalThoughtScore;

        @Schema(description = "자해 의도 점수", example = "5")
        private Integer selfHarmIntentScore;

        @Schema(description = "절망감 점수", example = "6")
        private Integer hopelessnessScore;

        @Schema(description = "설문 응답 내용", example = "{\"question1\":\"yes\",\"question2\":\"no\"}")
        private String responseData;

        @Schema(description = "위험도 판정 결과", example = "HIGH",
                allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
        private String riskLevel;

        @Schema(description = "설문자 메모", example = "최근 우울증 악화 추세")
        private String notes;

        @Schema(description = "생성 일시", example = "2024-01-15T14:30:00")
        private LocalDateTime createdAt;

        @Schema(description = "수정 일시", example = "2024-01-15T15:00:00")
        private LocalDateTime updatedAt;

        @Schema(description = "설문 완료 여부", example = "true")
        private Boolean isCompleted;
    }
}
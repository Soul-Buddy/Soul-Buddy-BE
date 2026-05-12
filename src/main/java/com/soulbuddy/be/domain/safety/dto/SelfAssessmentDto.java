// com/soulbuddy/be/domain/safety/dto/SelfAssessmentDto.java
package com.soulbuddy.be.domain.safety.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

public class SelfAssessmentDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotNull
        private Long userId;

        private String sessionId; // nullable

        @Builder.Default
        private String assessmentType = "SUICIDE_RISK";

        @NotNull
        private Map<String, String> answers; // {"q1":"NEVER", "q2":"OFTEN", ...}
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private Long id;
        private Long userId;
        private String sessionId;
        private String assessmentType;
        private Map<String, String> answers;
        private LocalDateTime createdAt;

        // DB 미저장 - 서버 계산 전용
        private int score;
        private String riskGrade; // LOW / MEDIUM / HIGH
    }
}

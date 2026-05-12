package com.soulbuddy.be.domain.safety.dto;

import com.soulbuddy.be.domain.safety.entity.Safetyevent;
import com.soulbuddy.be.domain.safety.entity.Safetyevent.RiskLevel;
import com.soulbuddy.be.domain.safety.entity.Safetyevent.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Schema(description = "Safety 이벤트 관련 DTO")
public class SafetyeventDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "Safety 이벤트 생성 요청")
    public static class CreateRequest {
        @NotNull(message = "사용자 ID는 필수입니다")
        @Schema(description = "사용자 ID", example = "123")
        private Long userId;

        @NotBlank(message = "세션 ID는 필수입니다")
        @Schema(description = "세션 ID", example = "SESSION-001")
        private String sessionId;

        @Schema(description = "메시지 ID (선택사항)", example = "456")
        private Long messageId;

        @NotNull(message = "이벤트 타입은 필수입니다")
        @Schema(description = "이벤트 타입", example = "RISK_DETECTED")
        private EventType eventType;

        @NotNull(message = "위험 수준은 필수입니다")
        @Schema(description = "위험 수준", example = "HIGH")
        private RiskLevel riskLevel;

        @Schema(description = "강제 안전 응답 여부", example = "false")
        private Boolean isForced;

        @Schema(description = "이벤트 설명", example = "자해 의도 표현 감지")
        private String eventDescription;

        public Safetyevent toEntity() {
            return Safetyevent.builder()
                    .userId(this.userId)
                    .sessionId(this.sessionId)
                    .messageId(this.messageId)
                    .eventType(this.eventType)
                    .riskLevel(this.riskLevel)
                    // 엔티티의 필드명이 forcedSafety이므로 여기에 맞춰 매핑
                    .forcedSafety(this.isForced != null && this.isForced)
                    // eventDescription은 엔티티에 필드가 있다면 추가 (현재 엔티티엔 없으므로 확인 필요)
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "Safety 이벤트 응답 정보")
    public static class Response {
        @Schema(description = "이벤트 고유 ID", example = "1")
        private Long id;
        @Schema(description = "이벤트 타입", example = "RISK_DETECTED")
        private EventType eventType;
        @Schema(description = "사용자 ID", example = "123")
        private Long userId;
        @Schema(description = "세션 ID", example = "SESSION-001")
        private String sessionId;
        @Schema(description = "위험 수준", example = "HIGH")
        private RiskLevel riskLevel;
        @Schema(description = "강제 여부", example = "true")
        private Boolean isForced;
        @Schema(description = "생성 일시", example = "2024-01-15T14:30:00")
        private LocalDateTime createdAt;

        public static Response from(Safetyevent entity) {
            return Response.builder()
                    .id(entity.getId())
                    .eventType(entity.getEventType())
                    .userId(entity.getUserId())
                    .sessionId(entity.getSessionId())
                    .riskLevel(entity.getRiskLevel())
                    .isForced(entity.getForcedSafety())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }
}
package com.soulbuddy.domain.safety.dto;

import com.soulbuddy.domain.safety.entity.Safetyevent;
import com.soulbuddy.domain.safety.entity.Safetyevent.EventType;
import com.soulbuddy.global.enums.RiskLevel; // ✅ 글로벌 Enum 사용
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Schema(description = "Safety 이벤트 관련 DTO")
public class SafetyeventDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        @NotNull @Schema(description = "사용자 ID", example = "123")
        private Long userId;

        @NotBlank @Schema(description = "세션 ID", example = "SESSION-001")
        private String sessionId;

        private Long messageId;

        @NotNull @Schema(description = "이벤트 타입")
        private EventType eventType;

        @NotNull @Schema(description = "위험 수준")
        private RiskLevel riskLevel; // ✅ 글로벌 RiskLevel

        private Boolean isForced;
        private String eventDescription;

        public Safetyevent toEntity() {
            return Safetyevent.builder()
                    .userId(this.userId)
                    .sessionId(this.sessionId)
                    .messageId(this.messageId)
                    .eventType(this.eventType)
                    .riskLevel(this.riskLevel)
                    .forcedSafety(this.isForced != null && this.isForced)
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private EventType eventType;
        private Long userId;
        private String sessionId;
        private RiskLevel riskLevel; // ✅ 글로벌 RiskLevel
        private Boolean isForced;
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
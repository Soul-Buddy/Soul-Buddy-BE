package com.soulbuddy.be.domain.safety.dto;

import com.soulbuddy.be.domain.safety.entity.Safetyevent;
import com.soulbuddy.be.domain.safety.entity.Safetyevent.EventType;
import com.soulbuddy.be.domain.safety.entity.Safetyevent.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class SafetyeventDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotNull(message = "user_id는 필수입니다.")
        private Long userId;

        @NotBlank(message = "session_id는 필수입니다.")
        private String sessionId;

        private Long messageId;

        private Long resourceId;

        @NotNull(message = "event_type은 필수입니다.")
        private EventType eventType;

        private RiskLevel riskLevel;

        @Builder.Default
        private Boolean forcedSafety = false;

        public Safetyevent toEntity() {
            return Safetyevent.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .messageId(messageId)
                    .resourceId(resourceId)
                    .eventType(eventType)
                    .riskLevel(riskLevel)
                    .forcedSafety(forcedSafety != null ? forcedSafety : false)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Response {

        private Long id;
        private Long userId;
        private String sessionId;
        private Long messageId;
        private Long resourceId;
        private EventType eventType;
        private RiskLevel riskLevel;
        private Boolean forcedSafety;
        private LocalDateTime createdAt;

        public static Response from(Safetyevent entity) {
            return Response.builder()
                    .id(entity.getId())
                    .userId(entity.getUserId())
                    .sessionId(entity.getSessionId())
                    .messageId(entity.getMessageId())
                    .resourceId(entity.getResourceId())
                    .eventType(entity.getEventType())
                    .riskLevel(entity.getRiskLevel())
                    .forcedSafety(entity.getForcedSafety())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }
}

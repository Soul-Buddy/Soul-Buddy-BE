package com.soulbuddy.domain.safety.dto;

import com.soulbuddy.domain.safety.entity.SafetyEvent;
import com.soulbuddy.global.enums.RiskLevel;
import com.soulbuddy.global.enums.SafetyEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Schema(description = "안전 이벤트 데이터 규격")
public class SafetyEventDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "안전 이벤트 생성 요청 (Request Body)")
    public static class CreateRequest {
        @NotBlank @Schema(description = "세션 ID", example = "uuid-string")
        private String sessionId;

        @Schema(description = "메시지 ID", example = "123")
        private Long messageId;

        @NotNull @Schema(description = "이벤트 타입", example = "BANNER_SHOWN")
        private SafetyEventType eventType;

        @Schema(description = "위험 수위", example = "MEDIUM")
        private RiskLevel riskLevel;

        @Schema(description = "리소스 ID (센터 ID 등)", example = "null")
        private Long resourceId;

        // 유저 정보는 세션이나 인증 정보에서 가져올 수 있으나,
        // 기존 코드 호환을 위해 필드가 필요하면 추가하세요.
        private Long userId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "안전 이벤트 응답 (Response Data)")
    public static class Response {
        @Schema(description = "생성된 이벤트 ID", example = "1")
        private Long eventId;

        @Schema(description = "생성 일시", example = "2026-05-13T10:10:00Z")
        private LocalDateTime createdAt;

        public static Response from(SafetyEvent entity) {
            return Response.builder()
                    .eventId(entity.getId())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }
}
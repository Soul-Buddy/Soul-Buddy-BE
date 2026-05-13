package com.soulbuddy.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "약관 동의 응답")
public record AgreementResponse(
        @Schema(description = "서비스 이용약관 동의 일시", example = "2026-05-04T10:00:00Z")
        LocalDateTime termsAgreedAt,

        @Schema(description = "개인정보 수집 및 이용 동의 일시", example = "2026-05-04T10:00:00Z")
        LocalDateTime privacyAgreedAt
) {
    /**
     * 필요 시 정적 팩토리 메서드를 유지하고 싶다면 아래와 같이 작성할 수 있습니다.
     * 계약서 스펙상 userId, email 등은 제외되었습니다.
     */
    public static AgreementResponse of(LocalDateTime termsAgreedAt, LocalDateTime privacyAgreedAt) {
        return new AgreementResponse(termsAgreedAt, privacyAgreedAt);
    }
}
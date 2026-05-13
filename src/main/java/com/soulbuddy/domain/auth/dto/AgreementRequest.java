package com.soulbuddy.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

@Schema(description = "약관 동의 요청")
public record AgreementRequest(
        @Schema(description = "서비스 이용약관 동의 여부 (반드시 true)", example = "true")
        @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
        boolean termsAgreed,

        @Schema(description = "개인정보 수집 및 이용 동의 여부 (반드시 true)", example = "true")
        @AssertTrue(message = "개인정보 수집 및 이용에 동의해야 합니다.")
        boolean privacyAgreed
) {}
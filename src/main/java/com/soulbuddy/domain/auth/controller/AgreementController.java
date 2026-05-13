package com.soulbuddy.domain.auth.controller;

import com.soulbuddy.domain.auth.dto.AgreementRequest;
import com.soulbuddy.domain.auth.dto.AgreementResponse;
import com.soulbuddy.domain.auth.service.AgreementService;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter; // 추가: Parameter 임포트
import io.swagger.v3.oas.annotations.responses.ApiResponses; // 추가: ApiResponses 임포트
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Agreement", description = "약관·동의 관련 API")
@RestController
@RequestMapping("/api/agreements")
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;

    @Operation(
            summary = "약관 동의 제출",
            description = "사용자의 이용약관 및 개인정보 동의 상태를 업데이트합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "동의 완료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수 약관 미동의 (VALID_001)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 토큰 만료 또는 없음 (AUTH_001)"
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<AgreementResponse>> agree(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal,
            @Valid @RequestBody AgreementRequest request
    ) {
        AgreementResponse response = agreementService.agree(Long.parseLong(principal), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "약관 동의 상태 조회", description = "현재 사용자의 약관 동의 날짜를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AgreementResponse>> getStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.getAgreementStatus(Long.parseLong(principal))));
    }
}
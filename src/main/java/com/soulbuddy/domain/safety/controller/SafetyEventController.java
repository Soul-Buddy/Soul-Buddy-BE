package com.soulbuddy.domain.safety.controller;

import com.soulbuddy.domain.safety.service.SafetyEventService;
import com.soulbuddy.global.enums.RiskLevel; // ✅ 글로벌 Enum 임포트
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Safety Event", description = "안전 이벤트 관리")
@RestController
@RequestMapping("/api/v1/safety-events")
@RequiredArgsConstructor
public class SafetyEventController {

    private final SafetyEventService safetyEventService;

    @Operation(summary = "위험 감지 기록", description = "위험 문구가 감지되었을 때 이벤트를 기록합니다.")
    @PostMapping("/risk-detected")
    public ResponseEntity<Void> recordRiskDetected(
            @RequestParam Long userId,
            @RequestParam String sessionId,
            @RequestParam(required = false) Long messageId,
            @RequestParam RiskLevel riskLevel, // ✅ 글로벌 타입으로 수신
            @RequestParam(defaultValue = "false") boolean isForced) {

        safetyEventService.recordRiskDetected(userId, sessionId, messageId, riskLevel, isForced);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
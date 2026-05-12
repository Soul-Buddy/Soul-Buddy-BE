package com.soulbuddy.be.domain.safety.controller;

import com.soulbuddy.be.domain.safety.dto.SafetyeventDto;
import com.soulbuddy.be.domain.safety.entity.Safetyevent.RiskLevel;
import com.soulbuddy.be.domain.safety.service.SafetyEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Safety Event API", description = "안전 이벤트 관리")
@RestController
@RequestMapping("/api/v1/safety-events")
@RequiredArgsConstructor
public class SafetyEventController {

    private final SafetyEventService safetyEventService;

    @Operation(summary = "범용 이벤트 기록", description = "DTO 형태의 요청으로 이벤트를 기록합니다.")
    @PostMapping
    public ResponseEntity<SafetyeventDto.Response> record(@Valid @RequestBody SafetyeventDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(safetyEventService.record(request));
    }

    @Operation(summary = "위험 감지 기록", description = "위험 감지 시 파라미터 기반으로 기록합니다.")
    @PostMapping("/risk-detected")
    public ResponseEntity<Void> recordRiskDetected(
            @RequestParam Long userId, @RequestParam String sessionId, @RequestParam(required = false) Long messageId,
            @RequestParam RiskLevel riskLevel, @RequestParam(defaultValue = "false") boolean isForced) {
        safetyEventService.recordRiskDetected(userId, sessionId, messageId, riskLevel, isForced);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "강제 안전 응답 기록")
    @PostMapping("/forced-safety")
    public ResponseEntity<Void> recordForcedSafety(
            @RequestParam Long userId, @RequestParam String sessionId, @RequestParam(required = false) Long messageId,
            @RequestParam(required = false) RiskLevel riskLevel, @RequestParam(defaultValue = "true") boolean isForced) {
        safetyEventService.recordForcedSafetyReply(userId, sessionId, messageId, riskLevel, isForced);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "사용자별 조회")
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<SafetyeventDto.Response>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(safetyEventService.getByUserId(userId));
    }

    @Operation(summary = "기간별 높은 위험 조회")
    @GetMapping("/high-risk")
    public ResponseEntity<List<SafetyeventDto.Response>> getHighRisk(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(safetyEventService.getHighRiskBetween(from, to));
    }

    @Operation(summary = "배너 노출 여부 확인")
    @GetMapping("/sessions/{sessionId}/banner-shown")
    public ResponseEntity<Boolean> isBannerShown(@PathVariable String sessionId) {
        return ResponseEntity.ok(safetyEventService.isBannerAlreadyShown(sessionId));
    }
}
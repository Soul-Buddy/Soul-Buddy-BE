package com.soulbuddy.be.domain.safety.controller;

import com.soulbuddy.be.domain.safety.dto.SafetyeventDto;
import com.soulbuddy.be.domain.safety.entity.Safetyevent.RiskLevel;
import com.soulbuddy.be.domain.safety.service.SafetyEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/safety-events")
@RequiredArgsConstructor
public class SafetyEventController {

    private final SafetyEventService safetyEventService;

    /**
     * POST /api/v1/safety-events
     * Safety 이벤트 기록 (범용)
     */
    @PostMapping
    public ResponseEntity<SafetyeventDto.Response> record(
            @Valid @RequestBody SafetyeventDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(safetyEventService.record(request));
    }

    /**
     * GET /api/v1/safety-events/{id}
     * 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<SafetyeventDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(safetyEventService.getById(id));
    }

    /**
     * GET /api/v1/safety-events?userId={userId}
     * 유저별 이벤트 조회
     */
    @GetMapping
    public ResponseEntity<List<SafetyeventDto.Response>> getByUserId(
            @RequestParam Long userId) {
        return ResponseEntity.ok(safetyEventService.getByUserId(userId));
    }

    /**
     * GET /api/v1/safety-events/sessions/{sessionId}
     * 세션별 이벤트 조회
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<List<SafetyeventDto.Response>> getBySessionId(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(safetyEventService.getBySessionId(sessionId));
    }

    /**
     * GET /api/v1/safety-events/high-risk?from=...&to=...
     * 기간별 HIGH 위험 이벤트 조회
     */
    @GetMapping("/high-risk")
    public ResponseEntity<List<SafetyeventDto.Response>> getHighRisk(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(safetyEventService.getHighRiskBetween(from, to));
    }

    /**
     * POST /api/v1/safety-events/risk-detected
     * HIGH 위험 감지 시 safety_events INSERT
     */
    @PostMapping("/risk-detected")
    public ResponseEntity<Void> recordRiskDetected(
            @RequestParam Long userId,
            @RequestParam String sessionId,
            @RequestParam(required = false) Long messageId,
            @RequestParam RiskLevel riskLevel,
            @RequestParam(defaultValue = "false") boolean isForced) {
        safetyEventService.recordRiskDetected(userId, sessionId, messageId, riskLevel, isForced);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * POST /api/v1/safety-events/forced-safety
     * 강제 안전 응답 시 safety_events INSERT
     */
    @PostMapping("/forced-safety")
    public ResponseEntity<Void> recordForcedSafety(
            @RequestParam Long userId,
            @RequestParam String sessionId,
            @RequestParam(required = false) Long messageId,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(defaultValue = "true") boolean isForced) {
        safetyEventService.recordForcedSafetyReply(userId, sessionId, messageId, riskLevel, isForced);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * GET /api/v1/safety-events/sessions/{sessionId}/banner-shown
     * 해당 세션에 이미 배너가 노출됐는지 확인
     */
    @GetMapping("/sessions/{sessionId}/banner-shown")
    public ResponseEntity<Boolean> isBannerShown(@PathVariable String sessionId) {
        return ResponseEntity.ok(safetyEventService.isBannerAlreadyShown(sessionId));
    }
}
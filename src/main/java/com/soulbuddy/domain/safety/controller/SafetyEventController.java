package com.soulbuddy.domain.safety.controller;

import com.soulbuddy.domain.safety.dto.SafetyEventDto;
import com.soulbuddy.domain.safety.entity.SafetyEvent;
import com.soulbuddy.domain.safety.service.SafetyEventService;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Safety", description = "위험 감지 및 안전 조치 API")
@RestController
@RequestMapping("/api/safety/events")
@RequiredArgsConstructor
public class SafetyEventController {

    private final SafetyEventService safetyEventService;

    @Operation(
            summary = "Safety 인터랙션 추적",
            description = "배너 노출, 센터 전화 클릭 등 FE 이벤트를 기록하고 eventId와 생성시간을 반환합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<SafetyEventDto.Response>> recordEvent(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal,
            @Valid @RequestBody SafetyEventDto.CreateRequest request) {

        request.setUserId(Long.parseLong(principal));
        SafetyEventDto.Response data = safetyEventService.recordCustomEvent(request);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(
            summary = "세션별 이벤트 목록 조회",
            description = "특정 세션에서 발생한 모든 안전 이벤트를 조회합니다."
    )
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<List<SafetyEventDto.Response>>> getEventsBySession(
            @PathVariable String sessionId) {

        // 엔티티 리스트를 DTO 리스트로 변환하여 반환
        List<SafetyEventDto.Response> data = safetyEventService.getEventsBySession(sessionId)
                .stream()
                .map(SafetyEventDto.Response::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
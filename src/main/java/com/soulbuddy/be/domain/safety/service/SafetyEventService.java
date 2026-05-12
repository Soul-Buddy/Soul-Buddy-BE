package com.soulbuddy.be.domain.safety.service;

import com.soulbuddy.be.domain.safety.dto.SafetyeventDto;
import com.soulbuddy.be.domain.safety.entity.Safetyevent;
import com.soulbuddy.be.domain.safety.entity.Safetyevent.EventType;
import com.soulbuddy.be.domain.safety.entity.Safetyevent.RiskLevel;
import com.soulbuddy.be.domain.safety.repository.SafetyeventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SafetyEventService {

    private final SafetyeventRepository safetyEventRepository;

    /**
     * Safety 이벤트 기록 (내부 공통 메서드)
     */
    @Transactional
    public SafetyeventDto.Response record(SafetyeventDto.CreateRequest request) {
        Safetyevent event = request.toEntity();
        Safetyevent saved = safetyEventRepository.save(event);

        log.info("[SafetyEvent] type={}, userId={}, sessionId={}, riskLevel={}, forcedSafety={}",
                saved.getEventType(), saved.getUserId(), saved.getSessionId(),
                saved.getRiskLevel(), saved.getForcedSafety());

        return SafetyeventDto.Response.from(saved);
    }

    /**
     * HIGH 위험 감지 시 safety_events INSERT
     */
    @Transactional
    public void recordRiskDetected(Long userId, String sessionId,
                                   Long messageId, RiskLevel riskLevel, boolean isForced) {
        SafetyeventDto.CreateRequest request = SafetyeventDto.CreateRequest.builder()
                .userId(userId)
                .sessionId(sessionId)
                .messageId(messageId)
                .eventType(EventType.RISK_DETECTED)
                .riskLevel(riskLevel)
                .forcedSafety(isForced)
                .build();
        record(request);
    }

    /**
     * 강제 안전 응답 시 safety_events INSERT
     */
    @Transactional
    public void recordForcedSafetyReply(Long userId, String sessionId,
                                        Long messageId, RiskLevel riskLevel, boolean isForced) {
        SafetyeventDto.CreateRequest request = SafetyeventDto.CreateRequest.builder()
                .userId(userId)
                .sessionId(sessionId)
                .messageId(messageId)
                .eventType(EventType.FORCED_SAFETY_REPLY)
                .riskLevel(riskLevel)
                .forcedSafety(isForced)
                .build();
        record(request);
    }

    /**
     * 단건 조회
     */
    public SafetyeventDto.Response getById(Long id) {
        Safetyevent event = safetyEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SafetyEvent not found: " + id));
        return SafetyeventDto.Response.from(event);
    }

    /**
     * 유저별 이벤트 전체 조회
     */
    public List<SafetyeventDto.Response> getByUserId(Long userId) {
        return safetyEventRepository.findByUserId(userId)
                .stream()
                .map(SafetyeventDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 세션별 이벤트 조회
     */
    public List<SafetyeventDto.Response> getBySessionId(String sessionId) {
        return safetyEventRepository.findBySessionId(sessionId)
                .stream()
                .map(SafetyeventDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 기간별 HIGH 위험 이벤트 조회
     */
    public List<SafetyeventDto.Response> getHighRiskBetween(LocalDateTime from, LocalDateTime to) {
        return safetyEventRepository.findHighRiskBetween(from, to)
                .stream()
                .map(SafetyeventDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 해당 세션에서 이미 배너가 노출됐는지 확인
     */
    public boolean isBannerAlreadyShown(String sessionId) {
        return safetyEventRepository.existsBySessionIdAndEventType(sessionId, EventType.BANNER_SHOWN);
    }
}
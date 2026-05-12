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

    @Transactional
    public SafetyeventDto.Response record(SafetyeventDto.CreateRequest request) {
        Safetyevent event = request.toEntity();
        Safetyevent saved = safetyEventRepository.save(event);
        log.info("[SafetyEvent Saved] ID: {}, Type: {}, User: {}", saved.getId(), saved.getEventType(), saved.getUserId());
        return SafetyeventDto.Response.from(saved);
    }

    @Transactional
    public void recordRiskDetected(Long userId, String sessionId, Long messageId, RiskLevel riskLevel, boolean isForced) {
        this.record(SafetyeventDto.CreateRequest.builder()
                .userId(userId).sessionId(sessionId).messageId(messageId)
                .eventType(EventType.RISK_DETECTED).riskLevel(riskLevel).isForced(isForced)
                .eventDescription("위험 문구 감지됨").build());
    }

    @Transactional
    public void recordForcedSafetyReply(Long userId, String sessionId, Long messageId, RiskLevel riskLevel, boolean isForced) {
        this.record(SafetyeventDto.CreateRequest.builder()
                .userId(userId).sessionId(sessionId).messageId(messageId)
                .eventType(EventType.FORCED_SAFETY_REPLY).riskLevel(riskLevel).isForced(isForced)
                .eventDescription("시스템에 의한 강제 안전 응답 전송").build());
    }

    public SafetyeventDto.Response getById(Long id) {
        return safetyEventRepository.findById(id).map(SafetyeventDto.Response::from)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + id));
    }

    public List<SafetyeventDto.Response> getByUserId(Long userId) {
        return safetyEventRepository.findByUserId(userId).stream()
                .map(SafetyeventDto.Response::from).collect(Collectors.toList());
    }

    public List<SafetyeventDto.Response> getBySessionId(String sessionId) {
        return safetyEventRepository.findBySessionId(sessionId).stream()
                .map(SafetyeventDto.Response::from).collect(Collectors.toList());
    }

    public List<SafetyeventDto.Response> getHighRiskBetween(LocalDateTime from, LocalDateTime to) {
        return safetyEventRepository.findHighRiskBetween(from, to).stream()
                .map(SafetyeventDto.Response::from).collect(Collectors.toList());
    }

    public boolean isBannerAlreadyShown(String sessionId) {
        return safetyEventRepository.existsBySessionIdAndEventType(sessionId, EventType.BANNER_SHOWN);
    }
}
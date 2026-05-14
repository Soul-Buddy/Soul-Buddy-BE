package com.soulbuddy.domain.safety.service;

import com.soulbuddy.domain.safety.dto.SafetyEventDto;
import com.soulbuddy.domain.safety.entity.SafetyEvent;
import com.soulbuddy.domain.safety.repository.SafetyEventRepository; // 대소문자 수정 확인
import com.soulbuddy.global.enums.RiskLevel;
import com.soulbuddy.global.enums.SafetyEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List; // List 임포트 추가

@Service
@RequiredArgsConstructor
public class SafetyEventService {

    private final SafetyEventRepository safetyEventRepository; // 대소문자 수정 확인

    /**
     * ChatService에서 HIGH 위험 감지 시 호출
     */
    @Transactional
    public void recordRiskDetected(Long userId, String sessionId, Long messageId, RiskLevel riskLevel, boolean forced) {
        save(userId, sessionId, messageId, SafetyEventType.RISK_DETECTED, riskLevel, forced);
    }

    /**
     * ChatService에서 강제 안전 응답 발생 시 호출
     */
    @Transactional
    public void recordForcedSafetyReply(Long userId, String sessionId, Long messageId, RiskLevel riskLevel, boolean forced) {
        save(userId, sessionId, messageId, SafetyEventType.FORCED_SAFETY_REPLY, riskLevel, forced);
    }

    /**
     * FE 인터랙션(배너 노출, 전화 클릭 등) 기록용
     */
    @Transactional
    public SafetyEventDto.Response recordCustomEvent(SafetyEventDto.CreateRequest request) {
        SafetyEvent event = SafetyEvent.builder()
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .messageId(request.getMessageId())
                .eventType(request.getEventType())
                .riskLevel(request.getRiskLevel())
                .resourceId(request.getResourceId())
                .forcedSafety(false)
                .build();

        SafetyEvent saved = safetyEventRepository.save(event);
        return SafetyEventDto.Response.from(saved);
    }

    /**
     * 특정 세션의 모든 안전 이벤트 조회
     */
    @Transactional(readOnly = true)
    public List<SafetyEvent> getEventsBySession(String sessionId) {
        return safetyEventRepository.findBySessionId(sessionId);
    }

    /**
     * 같은 세션에 강제 안전 발화 이벤트가 이미 발생했는지 확인.
     * v2.3 — HIGH 누적 임계치 도달 후 강제 안전 발화는 세션당 1회로 제한한다.
     */
    @Transactional(readOnly = true)
    public boolean hasForcedSafetyEmitted(String sessionId) {
        return safetyEventRepository.existsBySessionIdAndForcedSafetyTrue(sessionId);
    }

    // 공통 저장 로직
    private void save(Long userId, String sessionId, Long messageId, SafetyEventType type, RiskLevel level, boolean forced) {
        SafetyEvent event = SafetyEvent.builder()
                .userId(userId)
                .sessionId(sessionId)
                .messageId(messageId)
                .eventType(type)
                .riskLevel(level != null ? level : RiskLevel.LOW)
                .forcedSafety(forced)
                .build();
        safetyEventRepository.save(event);
    }
}
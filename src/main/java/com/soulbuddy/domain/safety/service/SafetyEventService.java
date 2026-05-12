package com.soulbuddy.domain.safety.service;

import com.soulbuddy.domain.safety.dto.SafetyeventDto;
import com.soulbuddy.domain.safety.entity.Safetyevent;
import com.soulbuddy.domain.safety.entity.Safetyevent.EventType;
import com.soulbuddy.domain.safety.repository.SafetyeventRepository;
import com.soulbuddy.global.enums.RiskLevel; // ✅ 글로벌 Enum 임포트 (타입 불일치 해결)
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
     * 공통 저장 로직: 모든 이벤트는 최종적으로 이 메서드를 통해 저장됩니다.
     */
    @Transactional
    public SafetyeventDto.Response record(SafetyeventDto.CreateRequest request) {
        Safetyevent event = request.toEntity();
        Safetyevent saved = safetyEventRepository.save(event);
        log.info("[SafetyEvent Saved] ID: {}, Type: {}, User: {}", saved.getId(), saved.getEventType(), saved.getUserId());
        return SafetyeventDto.Response.from(saved);
    }

    /**
     * [사진 요구사항 1] 위험 감지 전용 기록 로직 (HIGH 감지 시)
     */
    @Transactional
    public void recordRiskDetected(Long userId, String sessionId, Long messageId, RiskLevel riskLevel, boolean isForced) {
        this.record(SafetyeventDto.CreateRequest.builder()
                .userId(userId)
                .sessionId(sessionId)
                .messageId(messageId)
                .eventType(EventType.RISK_DETECTED)
                .riskLevel(riskLevel)
                .isForced(isForced)
                .eventDescription("위험 문구 감지됨")
                .build());
    }

    /**
     * [사진 요구사항 2] 강제 안전 응답 전용 기록 로직
     */
    @Transactional
    public void recordForcedSafetyReply(Long userId, String sessionId, Long messageId, RiskLevel riskLevel, boolean isForced) {
        this.record(SafetyeventDto.CreateRequest.builder()
                .userId(userId)
                .sessionId(sessionId)
                .messageId(messageId)
                .eventType(EventType.FORCED_SAFETY_REPLY)
                .riskLevel(riskLevel)
                .isForced(isForced)
                .eventDescription("시스템에 의한 강제 안전 응답 전송")
                .build());
    }

    // --- 조회 관련 메서드들 ---

    public SafetyeventDto.Response getById(Long id) {
        return safetyEventRepository.findById(id).map(SafetyeventDto.Response::from)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + id));
    }

    public List<SafetyeventDto.Response> getByUserId(Long userId) {
        return safetyEventRepository.findByUserId(userId).stream()
                .map(SafetyeventDto.Response::from)
                .collect(Collectors.toList());
    }

    public List<SafetyeventDto.Response> getHighRiskBetween(LocalDateTime from, LocalDateTime to) {
        return safetyEventRepository.findHighRiskBetween(from, to).stream()
                .map(SafetyeventDto.Response::from)
                .collect(Collectors.toList());
    }

    public boolean isBannerAlreadyShown(String sessionId) {
        return safetyEventRepository.existsBySessionIdAndEventType(sessionId, EventType.BANNER_SHOWN);
    }
}
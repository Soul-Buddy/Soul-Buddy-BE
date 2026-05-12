package com.soulbuddy.be.domain.safety.repository;

import com.soulbuddy.be.domain.safety.entity.Safetyevent;
import com.soulbuddy.be.domain.safety.entity.Safetyevent.EventType;
import com.soulbuddy.be.domain.safety.entity.Safetyevent.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SafetyeventRepository extends JpaRepository<Safetyevent, Long> {

    List<Safetyevent> findByUserId(Long userId);

    List<Safetyevent> findBySessionId(String sessionId);

    List<Safetyevent> findByUserIdAndEventType(Long userId, EventType eventType);

    List<Safetyevent> findByRiskLevel(RiskLevel riskLevel);

    @Query("SELECT e FROM Safetyevent e WHERE e.sessionId = :sessionId AND e.eventType = 'RISK_DETECTED'")
    List<Safetyevent> findRiskDetectedBySession(@Param("sessionId") String sessionId);

    @Query("SELECT e FROM Safetyevent e WHERE e.riskLevel = 'HIGH' AND e.createdAt BETWEEN :from AND :to")
    List<Safetyevent> findHighRiskBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    List<Safetyevent> findByForcedSafetyTrue();

    boolean existsBySessionIdAndEventType(String sessionId, EventType eventType);
}
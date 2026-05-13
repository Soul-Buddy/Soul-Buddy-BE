package com.soulbuddy.domain.safety.repository;

import com.soulbuddy.domain.safety.entity.Safetyevent;
import com.soulbuddy.domain.safety.entity.Safetyevent.EventType;
import com.soulbuddy.global.enums.RiskLevel; // ✅ 글로벌 Enum 사용
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface SafetyeventRepository extends JpaRepository<Safetyevent, Long> {
    List<Safetyevent> findByUserId(Long userId);
    List<Safetyevent> findBySessionId(String sessionId);
    List<Safetyevent> findByRiskLevel(RiskLevel riskLevel); // ✅ 글로벌 RiskLevel

    @Query("SELECT e FROM Safetyevent e WHERE e.riskLevel = com.soulbuddy.global.enums.RiskLevel.HIGH AND e.createdAt BETWEEN :from AND :to")
    List<Safetyevent> findHighRiskBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    boolean existsBySessionIdAndEventType(String sessionId, EventType eventType);
}
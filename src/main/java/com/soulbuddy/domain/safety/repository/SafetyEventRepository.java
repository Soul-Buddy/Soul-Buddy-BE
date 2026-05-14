package com.soulbuddy.domain.safety.repository;

import com.soulbuddy.domain.safety.entity.SafetyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SafetyEventRepository extends JpaRepository<SafetyEvent, Long> {
    List<SafetyEvent> findBySessionId(String sessionId);

    /**
     * 같은 세션에 강제 안전 발화(forcedSafety=true) 이벤트가 이미 한 번이라도 기록됐는지.
     * v2.3 — HIGH 누적 임계치 도달 후 1회만 강제 안전 발화하는 정책 판정에 사용.
     */
    boolean existsBySessionIdAndForcedSafetyTrue(String sessionId);
}
package com.soulbuddy.domain.safety.repository;

import com.soulbuddy.domain.safety.entity.SafetyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SafetyEventRepository extends JpaRepository<SafetyEvent, Long> {
    List<SafetyEvent> findBySessionId(String sessionId);
}
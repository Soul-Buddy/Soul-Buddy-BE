package com.soulbuddy.be.domain.emotion.repository;

import com.soulbuddy.be.domain.emotion.entity.EmotionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmotionLogRepository extends JpaRepository<EmotionLog, Long> {

    List<EmotionLog> findByUserId(Long userId);

    List<EmotionLog> findBySessionId(String sessionId);
}

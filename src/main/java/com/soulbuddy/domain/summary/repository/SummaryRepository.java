package com.soulbuddy.domain.summary.repository;

import com.soulbuddy.domain.summary.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SummaryRepository extends JpaRepository<Summary, Long> {

    Optional<Summary> findBySessionId(String sessionId);

    List<Summary> findAllBySessionIdIn(List<String> sessionIds);

    /**
     * 해당 사용자의 가장 최근(직전) 세션 요약 1건. recentSummary 자동 로드용.
     */
    Optional<Summary> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}

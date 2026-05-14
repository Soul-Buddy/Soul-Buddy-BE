package com.soulbuddy.domain.chat.repository;

import com.soulbuddy.domain.chat.entity.ChatMessage;
import com.soulbuddy.global.enums.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // GET /api/chat/history - 페이지네이션 조회
    Page<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId, Pageable pageable);

    // PATCH /api/sessions/{id}/end - 요약 생성용 전체 메시지
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    // POST /api/chat - PromptContext 최근 N개 (서비스에서 역순 후 reverse)
    List<ChatMessage> findTop20BySessionIdOrderByCreatedAtDesc(String sessionId);

    // POST /api/chat - recentHighCount 산출
    long countBySessionIdAndRiskLevel(String sessionId, RiskLevel riskLevel);

    // PR-2 v2.3 — last_compacted_message_id 이후 미요약 구간만 조회
    List<ChatMessage> findBySessionIdAndIdGreaterThanOrderByCreatedAtAsc(String sessionId, Long messageId);

    // PR-2 v2.3 — 미요약 구간 개수 (압축 트리거 판정용)
    long countBySessionIdAndIdGreaterThan(String sessionId, Long messageId);

    // PR-2 v2.3 — 미요약 구간 중 가장 오래된 N개 (압축 대상). Pageable 로 N 제어.
    List<ChatMessage> findBySessionIdAndIdGreaterThanOrderByIdAsc(String sessionId, Long messageId,
                                                                  org.springframework.data.domain.Pageable pageable);
}

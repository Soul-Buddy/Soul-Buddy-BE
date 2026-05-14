package com.soulbuddy.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 세션 내 슬라이딩 윈도우 압축 결과 (PR-2 / v2.3).
 *
 * 한 세션당 1행. 세션 메시지 수가 80개에 도달하면 가장 오래된 20개를
 * 네이버 요약 API 로 압축하여 본 테이블에 누적 저장.
 *
 * 페르소나 호출 시 system 프롬프트의 [세션 진행 요약] 블록에 부착되어 주입됨.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "chat_session_running_summary")
public class ChatSessionRunningSummary {

    @Id
    @Column(name = "session_id", length = 36, nullable = false)
    private String sessionId;

    @Column(name = "running_summary", columnDefinition = "TEXT")
    private String runningSummary;

    /**
     * 가장 최근 압축이 포함한 마지막 chat_messages.id.
     * 다음 압축은 이 id 초과 메시지부터 카운트. NULL = 아직 한 번도 압축 안 됨.
     */
    @Column(name = "last_compacted_message_id")
    private Long lastCompactedMessageId;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void apply(String newRunningSummary, Long newLastCompactedMessageId) {
        this.runningSummary = newRunningSummary;
        this.lastCompactedMessageId = newLastCompactedMessageId;
    }
}

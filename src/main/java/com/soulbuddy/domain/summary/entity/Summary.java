package com.soulbuddy.domain.summary.entity;

import com.soulbuddy.global.enums.EmotionTag;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "summaries")
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 36)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "summary_text", nullable = false, columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "situation_text", columnDefinition = "TEXT")
    private String situationText;

    @Column(name = "emotion_text", columnDefinition = "TEXT")
    private String emotionText;

    @Column(name = "thought_text", columnDefinition = "TEXT")
    private String thoughtText;

    @Enumerated(EnumType.STRING)
    @Column(name = "dominant_emotion", length = 20)
    private EmotionTag dominantEmotion;

    @Column(name = "emotion_distribution", columnDefinition = "JSON")
    private String emotionDistribution;

    @Column(name = "emotion_change", length = 255)
    private String emotionChange;

    @Column(name = "quote_text", length = 500)
    private String quoteText;

    @Column(name = "memory_hint", columnDefinition = "TEXT")
    private String memoryHint;

    /**
     * PR-6 — HCX-007 이 추출한 RAG 인덱싱용 키워드. 콤마 구분 (예: "학교, 시험, 자존감").
     * rag_chunks.pulling_text 원본이자 향후 사용자 키워드 대시보드 등 재활용 가능.
     */
    @Column(name = "keywords", length = 500)
    private String keywords;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

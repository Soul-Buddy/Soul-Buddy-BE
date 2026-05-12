package com.soulbuddy.be.domain.safety.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "self_assessments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SelfAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // FK -> chat_sessions, ON DELETE SET NULL
    @Column(name = "session_id", length = 36)
    private String sessionId;

    // DEFAULT 'SUICIDE_RISK'
    @Column(name = "assessment_type", length = 50)
    @Builder.Default
    private String assessmentType = "SUICIDE_RISK";

    // JSON 타입 - {"q1":"...", "q2":"..."}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers", nullable = false, columnDefinition = "json")
    private Map<String, String> answers;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

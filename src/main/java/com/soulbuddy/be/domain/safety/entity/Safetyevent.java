package com.soulbuddy.be.domain.safety.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "safety_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Safetyevent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "resource_id")
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column(name = "forced_safety", nullable = false)
    @Builder.Default
    private Boolean forcedSafety = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum EventType {
        RISK_DETECTED,
        BANNER_SHOWN,
        ASSESSMENT_OPENED,
        ASSESSMENT_COMPLETED,
        CENTER_LIST_VIEWED,
        CENTER_CALL_TAPPED,
        FORCED_SAFETY_REPLY
    }

    public enum RiskLevel {
        MEDIUM,
        HIGH
    }
}
package com.soulbuddy.domain.safety.entity;

import com.soulbuddy.global.enums.RiskLevel;
import com.soulbuddy.global.enums.SafetyEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "safety_events")
public class SafetyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    private Long messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private SafetyEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    private Long resourceId; // 추가됨: 센터 ID 등 기록용

    @Column(name = "forced_safety")
    private Boolean forcedSafety;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
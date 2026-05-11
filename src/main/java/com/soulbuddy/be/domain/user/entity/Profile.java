package com.soulbuddy.be.domain.user.entity;

import com.soulbuddy.be.global.converter.StringListConverter;
import com.soulbuddy.be.global.enums.PreferredTone;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 50)
    private String nickname;

    private Integer age;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> hobbies;

    @Column(columnDefinition = "TEXT")
    private String personality;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> concerns;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_tone", nullable = false, length = 50)
    private PreferredTone preferredTone;

    @Convert(converter = StringListConverter.class)
    @Column(name = "liked_things", columnDefinition = "TEXT")
    private List<String> likedThings;

    @Convert(converter = StringListConverter.class)
    @Column(name = "disliked_things", columnDefinition = "TEXT")
    private List<String> dislikedThings;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

package com.soulbuddy.domain.chat.repository;

import com.soulbuddy.domain.chat.entity.ChatSessionRunningSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatSessionRunningSummaryRepository
        extends JpaRepository<ChatSessionRunningSummary, String> {

    Optional<ChatSessionRunningSummary> findBySessionId(String sessionId);
}

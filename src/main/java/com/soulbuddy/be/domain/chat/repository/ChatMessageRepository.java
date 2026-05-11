package com.soulbuddy.be.domain.chat.repository;

import com.soulbuddy.be.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId, Pageable pageable);

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}

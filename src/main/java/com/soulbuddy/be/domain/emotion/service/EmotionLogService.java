package com.soulbuddy.be.domain.emotion.service;

import com.soulbuddy.be.domain.chat.entity.ChatSession;
import com.soulbuddy.be.domain.emotion.entity.EmotionLog;
import com.soulbuddy.be.domain.emotion.repository.EmotionLogRepository;
import com.soulbuddy.be.domain.user.entity.User;
import com.soulbuddy.be.domain.user.repository.UserRepository;
import com.soulbuddy.be.global.enums.EmotionTag;
import com.soulbuddy.be.global.exception.BusinessException;
import com.soulbuddy.be.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmotionLogService {

    private final EmotionLogRepository emotionLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void save(Long userId, ChatSession session, EmotionTag emotionTag) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        EmotionLog log = EmotionLog.builder()
                .user(user)
                .session(session)
                .emotionTag(emotionTag)
                .build();

        emotionLogRepository.save(log);
    }

    public List<EmotionLog> getByUserId(Long userId) {
        return emotionLogRepository.findByUserId(userId);
    }
}

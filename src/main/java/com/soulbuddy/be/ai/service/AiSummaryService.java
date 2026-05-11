package com.soulbuddy.be.ai.service;


import com.soulbuddy.be.ai.dto.SummaryResult;
import com.soulbuddy.be.domain.chat.entity.ChatMessage;

import java.util.List;

/**
 * 세션 종료 요약 생성 서비스
 * DB를 직접 호출하지 않습니다.
 */
public interface AiSummaryService {

    SummaryResult summarize(List<ChatMessage> messages);
}

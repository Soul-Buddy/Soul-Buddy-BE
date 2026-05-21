package com.soulbuddy.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulbuddy.ai.client.SummaryLlmClient;
import com.soulbuddy.ai.dto.ChatMessageDto;
import com.soulbuddy.ai.dto.SummaryInputContext;
import com.soulbuddy.ai.dto.SummaryResult;
import com.soulbuddy.ai.parser.AiResponseParser;
import com.soulbuddy.domain.rag.service.RagIndexingService;
import com.soulbuddy.domain.summary.entity.Summary;
import com.soulbuddy.domain.summary.repository.SummaryRepository;
import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.Sender;
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryServiceImpl implements AiSummaryService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final SummaryLlmClient summaryLlmClient;
    private final AiResponseParser aiResponseParser;
    private final SummaryRepository summaryRepository;
    private final ObjectMapper objectMapper;
    private final RagIndexingService ragIndexingService;

    @Value("${soulbuddy.rag.enabled:true}")
    private boolean ragEnabled;

    @Override
    @Transactional
    public SummaryResult summarize(SummaryInputContext context) {
        String userMessage = buildStructuredInput(context);
        long start = System.currentTimeMillis();
        String raw = summaryLlmClient.summarize(userMessage);
        log.info("HCX-007 요약 응답 시간: {}ms (sessionId={})",
                System.currentTimeMillis() - start, context.getSessionId());

        if (raw == null) {
            log.error("HCX-007 호출 실패 — raw=null (sessionId={})", context.getSessionId());
            throw new BusinessException(ErrorCode.AI_001);
        }
        SummaryResult result = aiResponseParser.parseSummary(raw);

        Summary saved = saveSummary(context.getSessionId(), context.getUserId(), result);
        if (ragEnabled && saved != null) {
            // PR-6 — 세션 종료 직후 RAG 인덱싱 (백그라운드, 응답 시간 영향 없음)
            ragIndexingService.indexSummary(saved);
        }
        return result;
    }

    /**
     * system_prompts_final.txt §7 입력 포맷:
     *   [세션 메타]
     *   persona: ...
     *   preChatEmotion: ...
     *   turnCount: ...
     *   sessionEmotionCounts: { HAPPY:0, SAD:0, ... }
     *
     *   [대화]
     *   (1) [createdAt ISO] 사용자: ...
     *   (2) [createdAt ISO] AI:    ...
     */
    String buildStructuredInput(SummaryInputContext ctx) {
        StringBuilder sb = new StringBuilder();

        sb.append("[세션 메타]\n");
        sb.append("persona: ").append(ctx.getPersona() != null ? ctx.getPersona().name() : "null").append('\n');
        sb.append("preChatEmotion: ")
          .append(ctx.getPreChatEmotion() != null ? ctx.getPreChatEmotion().name() : "null").append('\n');
        sb.append("turnCount: ").append(ctx.getTurnCount()).append('\n');
        sb.append("sessionEmotionCounts: ").append(formatEmotionCounts(ctx.getSessionEmotionCounts())).append("\n\n");

        sb.append("[대화]\n");
        List<ChatMessageDto> messages = ctx.getMessages();
        if (messages != null) {
            int idx = 1;
            for (ChatMessageDto m : messages) {
                String role = m.getSender() == Sender.USER ? "사용자"
                        : (m.getSender() == Sender.ASSISTANT ? "AI" : "시스템");
                String ts = m.getCreatedAt() != null ? m.getCreatedAt().format(ISO) : "";
                sb.append('(').append(idx++).append(") [").append(ts).append("] ")
                  .append(role).append(": ")
                  .append(m.getContent() != null ? m.getContent() : "")
                  .append('\n');
            }
        }
        return sb.toString();
    }

    private String formatEmotionCounts(Map<EmotionTag, Long> counts) {
        // 6종 모두 출력 (없는 키도 0). system prompt 의 6키 스키마와 일치시킴.
        Map<EmotionTag, Long> filled = new EnumMap<>(EmotionTag.class);
        for (EmotionTag t : EmotionTag.values()) {
            filled.put(t, 0L);
        }
        if (counts != null) {
            counts.forEach((k, v) -> { if (k != null && v != null) filled.put(k, v); });
        }
        StringBuilder sb = new StringBuilder("{ ");
        boolean first = true;
        for (Map.Entry<EmotionTag, Long> e : filled.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(e.getKey().name()).append(':').append(e.getValue());
            first = false;
        }
        sb.append(" }");
        return sb.toString();
    }

    private Summary saveSummary(String sessionId, Long userId, SummaryResult result) {
        String distributionJson = null;
        if (result.getEmotionDistribution() != null) {
            try {
                distributionJson = objectMapper.writeValueAsString(result.getEmotionDistribution());
            } catch (Exception e) {
                log.warn("emotionDistribution 직렬화 실패: {}", e.getMessage());
            }
        }
        return summaryRepository.save(Summary.builder()
                .sessionId(sessionId)
                .userId(userId)
                .summaryText(result.getSummaryText())
                .situationText(result.getSituationText())
                .emotionText(result.getEmotionText())
                .thoughtText(result.getThoughtText())
                .dominantEmotion(result.getDominantEmotion())
                .emotionDistribution(distributionJson)
                .emotionChange(result.getEmotionChange())
                .quoteText(result.getQuoteText())
                .memoryHint(result.getMemoryHint())
                .keywords(joinKeywords(result.getKeywords()))
                .build());
    }

    /** keywords 배열을 콤마 구분 String 으로 직렬화 (DB summaries.keywords 컬럼용, 500자 cap). */
    private static String joinKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return null;
        String joined = String.join(", ", keywords);
        return joined.length() > 500 ? joined.substring(0, 500) : joined;
    }
}

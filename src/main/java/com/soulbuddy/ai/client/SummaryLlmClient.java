package com.soulbuddy.ai.client;

import com.soulbuddy.ai.prompt.SystemPromptLoader;
import com.soulbuddy.global.config.ClovaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HCX-007 (베이스) 세션 요약 호출.
 *
 * v2.3 정합:
 *  - system prompt 는 system_prompts_final.txt §7 정본 (SystemPromptLoader 로드)
 *  - HCX-007 thinking 모델 전용 body 키 사용
 *      · maxCompletionTokens (≠ maxTokens)
 *      · thinking.effort = "high"
 *
 * 응답 형식: JSON {summaryText, situationText, emotionText, thoughtText,
 *                 dominantEmotion, emotionDistribution, emotionChange,
 *                 quoteText, memoryHint}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryLlmClient {

    private static final int MAX_COMPLETION_TOKENS = 32768;
    private static final double TEMPERATURE = 0.3;
    private static final String THINKING_EFFORT = "high";

    private final ClovaHttpClient clovaHttpClient;
    private final ClovaProperties clovaProperties;
    private final SystemPromptLoader systemPromptLoader;

    /**
     * HCX-007 요약 호출.
     * @param structuredUserMessage [세션 메타] / [대화] 두 블록으로 조립된 user 메시지.
     *                              AiSummaryServiceImpl.buildStructuredInput 결과.
     */
    public String summarize(String structuredUserMessage) {
        Map<String, Object> body = buildBody(structuredUserMessage);
        return clovaHttpClient.callJson(
                clovaProperties.getEndpoint().getSummary(),
                clovaProperties.getRequestId().getSummary(),
                body);
    }

    private Map<String, Object> buildBody(String userMessage) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPromptLoader.getSummarySystem()),
                Map.of("role", "user", "content", userMessage)
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages", messages);
        body.put("thinking", Map.of("effort", THINKING_EFFORT));
        body.put("topP", 0.8);
        body.put("topK", 0);
        body.put("maxCompletionTokens", MAX_COMPLETION_TOKENS);
        body.put("temperature", TEMPERATURE);
        body.put("repetitionPenalty", 1.1);
        body.put("seed", 0);
        body.put("includeAiFilters", true);
        return body;
    }
}

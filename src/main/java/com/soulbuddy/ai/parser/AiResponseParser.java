package com.soulbuddy.ai.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.ai.dto.SummaryResult;
import com.soulbuddy.ai.filter.SafetyFilter;
import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.RiskLevel;
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CLOVA 페르소나 응답은 자연어 텍스트만 반환하므로 JSON 파싱이 아닌 통과(passthrough)이며,
 * memoryHint truncate / 강제 안전 발화 응답 / HCX-007 요약 JSON 파싱을 담당합니다.
 * AI 호출/파싱 실패 시 fallback 없이 BusinessException 으로 즉시 실패합니다.
 */
@Slf4j
@Component
public class AiResponseParser {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int MEMORY_HINT_MAX_LENGTH = 200;

    /** 페르소나 응답 후처리: 빈 응답이면 AI_002 throw. */
    public String sanitizeAssistantMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            log.error("페르소나 LLM 응답이 비어 있음 — raw='{}'", raw);
            throw new BusinessException(ErrorCode.AI_002);
        }
        return raw.trim();
    }

    public String truncateMemoryHint(String memoryHint) {
        if (memoryHint == null) return null;
        if (memoryHint.length() <= MEMORY_HINT_MAX_LENGTH) return memoryHint;
        return memoryHint.substring(0, MEMORY_HINT_MAX_LENGTH) + "...";
    }

    public ChatResponse safetyResponse() {
        return ChatResponse.builder()
                .assistantMessage(SafetyFilter.SAFETY_MESSAGE)
                .emotionTag(EmotionTag.HURT)
                .riskLevel(RiskLevel.HIGH)
                .interventionType(null)
                .ragUsed(false)
                .aiModel("SYSTEM")
                .forcedSafety(true)
                .summary(null)
                .memoryHint(null)
                .recommendedAction(SafetyFilter.SAFETY_ACTION)
                .build();
    }

    /** HCX-007 요약 응답 (JSON 형식 강제) → SummaryResult. */
    public SummaryResult parseSummary(String rawJson) {
        try {
            JsonNode node = mapper.readTree(stripCodeFence(rawJson));
            String memoryHint = truncateMemoryHint(getTextOrNull(node, "memoryHint"));
            EmotionTag dominant = parseEmotion(getTextOrNull(node, "dominantEmotion"));

            Map<String, Integer> distribution = new LinkedHashMap<>();
            JsonNode dist = node.get("emotionDistribution");
            if (dist != null && dist.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = dist.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    distribution.put(e.getKey(), e.getValue().asInt(0));
                }
            }

            return SummaryResult.builder()
                    .summaryText(getTextOrNull(node, "summaryText"))
                    .situationText(getTextOrNull(node, "situationText"))
                    .emotionText(getTextOrNull(node, "emotionText"))
                    .thoughtText(getTextOrNull(node, "thoughtText"))
                    .dominantEmotion(dominant)
                    .emotionDistribution(distribution)
                    .emotionChange(getTextOrNull(node, "emotionChange"))
                    .quoteText(getTextOrNull(node, "quoteText"))
                    .memoryHint(memoryHint)
                    .keywords(parseKeywords(node.get("keywords")))
                    .build();
        } catch (Exception e) {
            log.error("HCX-007 요약 응답 파싱 실패 — raw='{}', err={}", rawJson, e.getMessage());
            throw new BusinessException(ErrorCode.AI_002);
        }
    }

    /**
     * LLM 이 JSON 응답을 ```json ... ``` 마크다운 코드블록으로 감싸는 경우 벗긴다.
     * 코드블록이 없으면 원본 그대로 반환.
     */
    private static String stripCodeFence(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (!t.startsWith("```")) return t;
        // 첫 줄 (```json 또는 ```) 제거
        int firstNl = t.indexOf('\n');
        if (firstNl < 0) return t;
        t = t.substring(firstNl + 1);
        // 마지막 ``` 제거
        int lastFence = t.lastIndexOf("```");
        if (lastFence >= 0) t = t.substring(0, lastFence);
        return t.trim();
    }

    /** HCX-007 응답의 keywords 배열을 List<String> 으로 파싱. null/비배열은 빈 리스트. */
    private static List<String> parseKeywords(JsonNode node) {
        if (node == null || !node.isArray()) return Collections.emptyList();
        List<String> out = new ArrayList<>(node.size());
        for (JsonNode v : node) {
            if (v == null || v.isNull()) continue;
            String s = v.asText();
            if (s == null) continue;
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    private static EmotionTag parseEmotion(String s) {
        if (s == null) return null;
        return EmotionTag.fromJson(s);
    }

    private static String getTextOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || "null".equals(value.asText())) return null;
        return value.asText();
    }
}

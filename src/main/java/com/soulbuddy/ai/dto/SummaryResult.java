package com.soulbuddy.ai.dto;

import com.soulbuddy.global.enums.EmotionTag;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class SummaryResult {

    private String summaryText;
    private String situationText;
    private String emotionText;
    private String thoughtText;
    private EmotionTag dominantEmotion;
    private Map<String, Integer> emotionDistribution;
    private String emotionChange;
    private String quoteText;
    private String memoryHint;
    /** PR-6 — HCX-007 이 추출한 RAG 인덱싱용 핵심 키워드 (5~10개). */
    private List<String> keywords;
}

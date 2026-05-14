package com.soulbuddy.domain.rag.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PR-6 — RAG 검색 트리거 키워드 매칭기.
 *
 * 사용자가 명시적으로 과거 대화를 회상하려는 표현 ("기억나/지난번/예전에/말했던
 * /얘기했던/저번에/그때" 등) 이 발화에 포함된 경우에만 RAG 검색을 수행한다.
 * 모든 발화에 RAG 를 돌리면 토큰 비용 + 무관 인용 위험이 크기 때문에 트리거 기반
 * 호출이 정책 (vibe-coding-docs / AI_Logic_v2 명시).
 *
 * 키워드 목록은 application.yml soulbuddy.rag.trigger-keywords 로 외부화.
 */
@Slf4j
@Component
public class RagTriggerDetector {

    private final String rawKeywords;
    private List<String> keywords;

    public RagTriggerDetector(
            @Value("${soulbuddy.rag.trigger-keywords:기억나,지난번,예전에,말했던,얘기했던,저번에,그때,예전,지난}")
            String rawKeywords
    ) {
        this.rawKeywords = rawKeywords;
    }

    @PostConstruct
    void init() {
        this.keywords = Arrays.stream(rawKeywords.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableList());
        log.info("RagTriggerDetector 초기화 — 키워드 {}개: {}", keywords.size(), keywords);
    }

    /** 발화에 트리거 키워드가 하나라도 포함되어 있으면 true. */
    public boolean isTriggered(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return false;
        if (keywords == null || keywords.isEmpty()) return false;
        for (String kw : keywords) {
            if (userMessage.contains(kw)) return true;
        }
        return false;
    }

    /** 테스트용 — 등록된 키워드 목록 노출. */
    public List<String> getKeywords() {
        return keywords;
    }
}

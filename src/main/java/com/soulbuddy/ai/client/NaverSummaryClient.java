package com.soulbuddy.ai.client;

import com.soulbuddy.global.config.ClovaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 네이버 CLOVA Studio 요약 API 호출 클라이언트 (PR-2 / v2.3).
 *
 * Endpoint: POST /v1/api-tools/summarization/v2
 *
 * 용도: 세션 내 슬라이딩 윈도우 압축 — 가장 오래된 N개 메시지를 한 텍스트로 join
 *       해서 본 API 로 압축. 결과는 chat_session_running_summary 에 누적.
 *
 * ※ 본 클라이언트는 HCX-005/007 페르소나/요약 모델과는 다른 API 임에 주의.
 *   chat-completions 가 아닌 api-tools/summarization 경로.
 *   응답 JSON 의 result.text 가 요약 결과 (chat-completions 의
 *   result.message.content 와 구조 다름).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaverSummaryClient {

    private static final int SEG_MIN_SIZE = 300;
    private static final int SEG_MAX_SIZE = 1000;
    private static final int SEG_COUNT = -1;          // 모델이 최적 문단 수로 분리
    private static final boolean AUTO_SENTENCE_SPLITTER = true;
    private static final boolean INCLUDE_AI_FILTERS = true;

    private final ClovaHttpClient clovaHttpClient;
    private final ClovaProperties clovaProperties;

    /**
     * 입력 텍스트 1건을 압축. 최대 35,000자.
     * 응답 JSON 의 result.text 를 그대로 반환. 실패 시 null.
     */
    public String summarize(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("texts", List.of(text));
        body.put("autoSentenceSplitter", AUTO_SENTENCE_SPLITTER);
        body.put("segCount", SEG_COUNT);
        body.put("segMaxSize", SEG_MAX_SIZE);
        body.put("segMinSize", SEG_MIN_SIZE);
        body.put("includeAiFilters", INCLUDE_AI_FILTERS);

        return clovaHttpClient.callSummarization(
                clovaProperties.getEndpoint().getSummarization(),
                clovaProperties.getRequestId().getSummarization(),
                body);
    }
}

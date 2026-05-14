package com.soulbuddy.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulbuddy.global.config.ClovaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 네이버 Clova RAG Reasoning API (/v1/api-tools/rag-reasoning) 호출 클라이언트.
 *
 * 본 API 는 RAG 의 "질문 분석 / 검색어 정제" 단계만 담당한다 — 사용자 발화를
 * 입력으로 받아 검색에 적합한 키워드/질문으로 정제한 뒤 toolCalls 로 반환한다.
 * 실제 문서 검색은 BE 가 정제된 query 를 받아 rag_chunks 테이블의 FULLTEXT
 * 인덱스로 직접 수행한다 (RagSearchService).
 *
 * 응답 구조 (성공):
 *   result.message.toolCalls[0].function.arguments.query  ← 정제된 검색어
 *
 * 정보 부족 등으로 toolCalls 가 비어 있으면 null 반환.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaverRagReasoningClient {

    private static final ObjectMapper mapper = new ObjectMapper();

    /** 소울버디 과거 대화 검색용 tool 정의. 매 호출 동일하므로 상수로 보관. */
    private static final Map<String, Object> PAST_CONVERSATION_TOOL = buildTool();

    private final WebClient clovaWebClient;
    private final ClovaProperties clovaProperties;

    /**
     * 사용자 발화에서 과거 대화 검색용 query 를 추출한다.
     *
     * @param userMessage 사용자 발화 원문 ("저번에 학교 얘기했던 거 기억나?" 등)
     * @return 정제된 검색어 (예: "학교"). 실패 / 비호출 시 null.
     */
    public String extractSearchQuery(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return null;

        String endpoint = clovaProperties.getEndpoint().getRagReasoning();
        String requestId = clovaProperties.getRequestId().getRagReasoning();
        if (endpoint == null || requestId == null) {
            log.warn("RAG Reasoning endpoint/requestId 미설정 — RAG 검색 스킵");
            return null;
        }

        Map<String, Object> body = buildBody(userMessage);

        try {
            JsonNode response = clovaWebClient.post()
                    .uri(endpoint)
                    .header("X-NCP-CLOVASTUDIO-REQUEST-ID", requestId)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(clovaProperties.getTimeoutSeconds()))
                    .retryWhen(Retry.backoff(clovaProperties.getMaxRetries(), Duration.ofMillis(500))
                            .filter(this::isRetryable))
                    .onErrorResume(e -> {
                        log.error("RAG Reasoning 호출 실패 endpoint={} err={}", endpoint, e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response == null) return null;
            JsonNode status = response.get("status");
            if (status != null && status.has("code")
                    && !"20000".equals(status.get("code").asText())) {
                log.warn("RAG Reasoning 비정상 응답 status={}", status);
                return null;
            }
            JsonNode result = response.get("result");
            if (result == null) return null;
            JsonNode message = result.get("message");
            if (message == null) return null;
            JsonNode toolCalls = message.get("toolCalls");
            if (toolCalls == null || !toolCalls.isArray() || toolCalls.isEmpty()) {
                log.debug("RAG Reasoning toolCalls 비어 있음 — 검색 스킵");
                return null;
            }
            JsonNode firstCall = toolCalls.get(0);
            JsonNode function = firstCall.get("function");
            if (function == null) return null;
            JsonNode arguments = function.get("arguments");
            if (arguments == null) return null;
            // arguments 는 object 또는 stringified JSON 둘 다 올 수 있음 — 둘 다 처리
            JsonNode queryNode = arguments.isObject()
                    ? arguments.get("query")
                    : tryParseJson(arguments.asText()).path("query");
            if (queryNode == null || queryNode.isNull() || queryNode.isMissingNode()) return null;
            String query = queryNode.asText();
            return query == null || query.isBlank() ? null : query.trim();
        } catch (Exception e) {
            log.error("RAG Reasoning 호출 예외 err={}", e.getMessage(), e);
            return null;
        }
    }

    private Map<String, Object> buildBody(String userMessage) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages", List.of(Map.of(
                "role", "user",
                "content", userMessage
        )));
        body.put("tools", List.of(PAST_CONVERSATION_TOOL));
        body.put("toolChoice", "auto");
        body.put("topP", 0.8);
        body.put("topK", 0);
        body.put("maxTokens", 1024);
        body.put("temperature", 0.5);
        body.put("repetitionPenalty", 1.1);
        body.put("stop", List.of());
        body.put("seed", 0);
        body.put("includeAiFilters", true);
        return body;
    }

    private static Map<String, Object> buildTool() {
        Map<String, Object> queryProp = new LinkedHashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description",
                "사용자가 떠올리려 하는 과거 대화의 핵심 주제 / 키워드. "
                        + "고유명사 / 닉네임은 제외하고, 한국어 일반 명사 또는 짧은 구로 작성. "
                        + "예: '학교', '시험 불안', '친구 갈등'");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("query", queryProp);

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", List.of("query"));

        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", "past_conversation_retrieval");
        function.put("description",
                "사용자가 과거에 나눈 정서 상담 대화 중 현재 발화와 관련된 내용을 찾을 때 사용합니다.\n"
                        + "'기억나/지난번/예전에/말했던/얘기했던/저번에/그때' 같은 표현이 등장하면 호출하세요.\n"
                        + "관련 과거 대화가 없거나 정보가 부족하면 호출하지 마세요.");
        function.put("parameters", parameters);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("function", function);
        return tool;
    }

    private static JsonNode tryParseJson(String s) {
        try {
            return mapper.readTree(s);
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    private boolean isRetryable(Throwable t) {
        String msg = t.getMessage();
        return msg != null && (msg.contains("timeout") || msg.contains("502") || msg.contains("503"));
    }
}

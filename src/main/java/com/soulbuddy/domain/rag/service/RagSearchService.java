package com.soulbuddy.domain.rag.service;

import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.domain.rag.entity.RagChunk;
import com.soulbuddy.domain.rag.repository.RagChunkRepository;
import com.soulbuddy.domain.summary.entity.Summary;
import com.soulbuddy.domain.summary.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * PR-6 — RAG 검색 서비스.
 *
 * RagTriggerDetector 가 트리거 hit 한 경우에만 호출된다. 흐름:
 *   1) NaverRagReasoningClient 가 정제한 query 를 받아
 *   2) rag_chunks FULLTEXT 인덱스로 같은 user_id 의 과거 청크 검색
 *   3) summary_id 기준으로 dedupe → 서로 다른 과거 세션 Top-K(기본 2개)
 *   4) 각 세션의 Summary 본문(상황/감정/사고/일자)을 RagChunkRef 로 변환
 *
 * 결과는 PromptBuilder.buildUserMessage 의 d 블록 [관련 과거 대화] 에 주입.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagSearchService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RagChunkRepository ragChunkRepository;
    private final SummaryRepository summaryRepository;

    @Value("${soulbuddy.rag.top-k:2}")
    private int topK;

    @Value("${soulbuddy.rag.min-query-length:2}")
    private int minQueryLength;

    /**
     * 정제된 query 로 과거 세션 요약 Top-K 를 PromptContext.RagChunkRef 리스트로 반환.
     *
     * @param userId            현재 사용자
     * @param currentSessionId  현재 진행 중인 세션 (자기 자신 매칭 방지)
     * @param query             RAG Reasoning 이 추출한 검색어
     * @return 비어 있을 수 있음. null 절대 반환 안 함.
     */
    @Transactional(readOnly = true)
    public List<PromptContext.RagChunkRef> searchTopK(Long userId, String currentSessionId, String query) {
        if (userId == null || currentSessionId == null || query == null) return Collections.emptyList();
        String trimmed = query.trim();
        if (trimmed.length() < minQueryLength) {
            log.debug("RAG query 너무 짧음 ({}자) — 검색 스킵: '{}'", trimmed.length(), trimmed);
            return Collections.emptyList();
        }

        // FULLTEXT 검색은 dedupe 후 topK 개를 채우기 위해 넉넉히 topK*3 만큼 끌어온다
        // (한 세션당 최대 3 청크 — situation/emotion/thought).
        int fetchSize = Math.max(topK * 3, topK);
        List<RagChunk> hits = ragChunkRepository.searchFullText(
                userId, currentSessionId, trimmed, PageRequest.of(0, fetchSize));

        if (hits.isEmpty()) {
            log.debug("RAG 검색 결과 0건 — userId={} query='{}'", userId, trimmed);
            return Collections.emptyList();
        }

        // summary_id 단위 dedupe — 같은 세션의 청크가 3개 다 매칭되어 같은 요약을
        // 반복 인용하는 상황 방지. 점수 높은 청크의 summary_id 우선.
        Set<Long> seenSummaryIds = new HashSet<>();
        List<Long> orderedSummaryIds = new ArrayList<>();
        for (RagChunk c : hits) {
            Long sid = c.getSummaryId();
            if (sid == null) continue;
            if (seenSummaryIds.add(sid)) {
                orderedSummaryIds.add(sid);
                if (orderedSummaryIds.size() >= topK) break;
            }
        }
        if (orderedSummaryIds.isEmpty()) return Collections.emptyList();

        List<PromptContext.RagChunkRef> refs = new ArrayList<>(orderedSummaryIds.size());
        for (Long summaryId : orderedSummaryIds) {
            Optional<Summary> opt = summaryRepository.findById(summaryId);
            if (opt.isEmpty()) continue;
            Summary s = opt.get();
            refs.add(PromptContext.RagChunkRef.builder()
                    .date(s.getCreatedAt() != null ? s.getCreatedAt().format(DATE_FMT) : null)
                    .situation(s.getSituationText())
                    .emotion(s.getEmotionText())
                    .thought(s.getThoughtText())
                    .build());
        }
        log.info("RAG 검색 성공 — userId={} query='{}' resultCount={}", userId, trimmed, refs.size());
        return refs;
    }
}

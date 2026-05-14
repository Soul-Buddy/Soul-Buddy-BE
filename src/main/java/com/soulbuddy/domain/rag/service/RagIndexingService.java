package com.soulbuddy.domain.rag.service;

import com.soulbuddy.domain.rag.entity.RagChunk;
import com.soulbuddy.domain.rag.repository.RagChunkRepository;
import com.soulbuddy.domain.summary.entity.Summary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.soulbuddy.global.config.AsyncConfig.AI_BACKGROUND_EXECUTOR;

/**
 * PR-6 — 세션 종료 시 Summary 를 rag_chunks 에 인덱싱.
 *
 * 한 세션당 3개 청크 (situation / emotion / thought). pulling_text 는 HCX-007
 * 이 추출한 keywords 콤마 구분 문자열 (공통). 향후 사용자가 "기억나? ~" 라고
 * 물으면 RagSearchService 가 FULLTEXT 검색으로 이 청크들을 끌어온다.
 *
 * @Async 백그라운드 실행 — 세션 종료 응답 시간에 영향 없음.
 * 실패 시 silently no-op (다음 세션의 인덱싱은 정상 진행, 본 세션만 검색 불가).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagIndexingService {

    private final RagChunkRepository ragChunkRepository;

    @Async(AI_BACKGROUND_EXECUTOR)
    @Transactional
    public void indexSummary(Summary summary) {
        if (summary == null || summary.getId() == null) {
            log.warn("RAG 인덱싱 스킵 — summary or summary.id null");
            return;
        }

        String pullingText = summary.getKeywords();   // 콤마 구분 키워드 — 공통
        LocalDateTime now = LocalDateTime.now();

        List<RagChunk> chunks = new ArrayList<>(3);
        appendIfNotBlank(chunks, summary, "[상황] " + safe(summary.getSituationText()), pullingText, now);
        appendIfNotBlank(chunks, summary, "[감정] " + safe(summary.getEmotionText()), pullingText, now);
        appendIfNotBlank(chunks, summary, "[사고] " + safe(summary.getThoughtText()), pullingText, now);

        if (chunks.isEmpty()) {
            log.info("RAG 인덱싱 — summaryId={} 청크 본문 모두 비어 있어 스킵", summary.getId());
            return;
        }

        try {
            ragChunkRepository.saveAll(chunks);
            log.info("RAG 인덱싱 완료 — summaryId={} userId={} sessionId={} 청크 {}개",
                    summary.getId(), summary.getUserId(), summary.getSessionId(), chunks.size());
        } catch (Exception e) {
            log.error("RAG 인덱싱 실패 — summaryId={} err={}", summary.getId(), e.getMessage(), e);
        }
    }

    private static void appendIfNotBlank(List<RagChunk> chunks, Summary summary,
                                         String chunkText, String pullingText, LocalDateTime now) {
        if (chunkText == null) return;
        // "[상황] " prefix 만 있고 본문 비어 있으면 의미 없는 청크 — 스킵
        String body = chunkText.replaceFirst("^\\[(상황|감정|사고)] ", "").trim();
        if (body.isEmpty()) return;

        chunks.add(RagChunk.builder()
                .userId(summary.getUserId())
                .sessionId(summary.getSessionId())
                .summaryId(summary.getId())
                .chunkText(chunkText)
                .pullingText(pullingText)
                .indexedAt(now)
                .build());
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

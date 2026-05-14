package com.soulbuddy.domain.rag.service;

import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.domain.rag.entity.RagChunk;
import com.soulbuddy.domain.rag.repository.RagChunkRepository;
import com.soulbuddy.domain.summary.entity.Summary;
import com.soulbuddy.domain.summary.repository.SummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PR-6 RagSearchService 검증.
 *  - 정상 검색 — 서로 다른 summary_id 청크 → 각각 RagChunkRef 반환
 *  - 같은 summary_id 청크 3개 hit → 1개로 dedupe
 *  - query 길이 부족 → 검색 스킵
 */
@ExtendWith(MockitoExtension.class)
class RagSearchServiceTest {

    @Mock private RagChunkRepository ragChunkRepository;
    @Mock private SummaryRepository summaryRepository;

    @InjectMocks
    private RagSearchService ragSearchService;

    private static final Long USER_ID = 1L;
    private static final String CURRENT_SESSION = "current-sess";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ragSearchService, "topK", 2);
        ReflectionTestUtils.setField(ragSearchService, "minQueryLength", 2);
    }

    @Test
    @DisplayName("정상 — 서로 다른 summary_id 청크 → 각각 RagChunkRef 반환")
    void searchTopK_returnsRefsForDistinctSummaries() {
        RagChunk c1 = RagChunk.builder().userId(USER_ID).sessionId("s1").summaryId(10L)
                .chunkText("[상황] 학교").build();
        RagChunk c2 = RagChunk.builder().userId(USER_ID).sessionId("s2").summaryId(20L)
                .chunkText("[감정] 불안").build();
        given(ragChunkRepository.searchFullText(eq(USER_ID), eq(CURRENT_SESSION), eq("학교"), any(Pageable.class)))
                .willReturn(List.of(c1, c2));

        Summary s10 = Summary.builder().id(10L).sessionId("s1").userId(USER_ID)
                .summaryText("요약").situationText("학교 압박").emotionText("불안").thoughtText("힘들다").build();
        ReflectionTestUtils.setField(s10, "createdAt", LocalDateTime.of(2026, 5, 10, 14, 0));
        Summary s20 = Summary.builder().id(20L).sessionId("s2").userId(USER_ID)
                .summaryText("요약2").situationText("친구 갈등").emotionText("상처").thoughtText("외롭다").build();
        ReflectionTestUtils.setField(s20, "createdAt", LocalDateTime.of(2026, 5, 12, 9, 30));

        given(summaryRepository.findById(10L)).willReturn(Optional.of(s10));
        given(summaryRepository.findById(20L)).willReturn(Optional.of(s20));

        List<PromptContext.RagChunkRef> refs =
                ragSearchService.searchTopK(USER_ID, CURRENT_SESSION, "학교");

        assertThat(refs).hasSize(2);
        assertThat(refs.get(0).getSituation()).isEqualTo("학교 압박");
        assertThat(refs.get(0).getDate()).isEqualTo("2026-05-10");
        assertThat(refs.get(1).getSituation()).isEqualTo("친구 갈등");
    }

    @Test
    @DisplayName("dedupe — 같은 summary_id 청크 3개 hit → 1개만 반환")
    void searchTopK_dedupesBySummaryId() {
        RagChunk c1 = RagChunk.builder().userId(USER_ID).sessionId("s1").summaryId(10L).build();
        RagChunk c2 = RagChunk.builder().userId(USER_ID).sessionId("s1").summaryId(10L).build();
        RagChunk c3 = RagChunk.builder().userId(USER_ID).sessionId("s1").summaryId(10L).build();
        given(ragChunkRepository.searchFullText(eq(USER_ID), eq(CURRENT_SESSION), eq("학교"), any(Pageable.class)))
                .willReturn(List.of(c1, c2, c3));

        Summary s10 = Summary.builder().id(10L).sessionId("s1").userId(USER_ID)
                .situationText("학교").build();
        given(summaryRepository.findById(10L)).willReturn(Optional.of(s10));

        List<PromptContext.RagChunkRef> refs =
                ragSearchService.searchTopK(USER_ID, CURRENT_SESSION, "학교");

        assertThat(refs).hasSize(1);
        assertThat(refs.get(0).getSituation()).isEqualTo("학교");
    }

    @Test
    @DisplayName("query 길이 부족 — 검색 스킵, repository 호출 안 함")
    void searchTopK_skipWhenQueryTooShort() {
        List<PromptContext.RagChunkRef> refs =
                ragSearchService.searchTopK(USER_ID, CURRENT_SESSION, "ㅇ");

        assertThat(refs).isEmpty();
        verify(ragChunkRepository, never())
                .searchFullText(anyLong(), anyString(), anyString(), any(Pageable.class));
    }
}

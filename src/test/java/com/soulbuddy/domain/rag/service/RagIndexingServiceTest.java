package com.soulbuddy.domain.rag.service;

import com.soulbuddy.domain.rag.entity.RagChunk;
import com.soulbuddy.domain.rag.repository.RagChunkRepository;
import com.soulbuddy.domain.summary.entity.Summary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PR-6 RagIndexingService 검증.
 *  - 3요소 모두 채워진 Summary → 3 청크 저장 + pullingText 일관
 *  - 빈 본문 요소는 청크 미생성
 *  - 모두 빈 경우 saveAll 미호출
 *  - summary == null / id == null 가드
 */
@ExtendWith(MockitoExtension.class)
class RagIndexingServiceTest {

    @Mock private RagChunkRepository ragChunkRepository;

    @InjectMocks
    private RagIndexingService ragIndexingService;

    @Test
    @DisplayName("3요소 모두 채워짐 → 청크 3개 저장 + pullingText 동일")
    void indexSummary_allThreeChunks() {
        Summary s = Summary.builder()
                .id(100L).userId(1L).sessionId("s-1")
                .situationText("학교에서 시험 압박").emotionText("불안과 자책")
                .thoughtText("실패할 것 같다")
                .keywords("학교, 시험, 자책, 불안")
                .build();

        ragIndexingService.indexSummary(s);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RagChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(ragChunkRepository).saveAll(captor.capture());

        List<RagChunk> saved = captor.getValue();
        assertThat(saved).hasSize(3);
        assertThat(saved).allMatch(c -> "학교, 시험, 자책, 불안".equals(c.getPullingText()));
        assertThat(saved).allMatch(c -> c.getSummaryId().equals(100L));
        assertThat(saved.stream().map(RagChunk::getChunkText))
                .anyMatch(t -> t.startsWith("[상황]"))
                .anyMatch(t -> t.startsWith("[감정]"))
                .anyMatch(t -> t.startsWith("[사고]"));
    }

    @Test
    @DisplayName("빈 본문 요소는 청크 미생성 — situation 비어 있으면 [상황] 스킵")
    void indexSummary_skipBlankFields() {
        Summary s = Summary.builder()
                .id(101L).userId(1L).sessionId("s-2")
                .situationText("   ").emotionText("불안").thoughtText("힘들다")
                .keywords("불안")
                .build();

        ragIndexingService.indexSummary(s);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RagChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(ragChunkRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().stream().map(RagChunk::getChunkText))
                .noneMatch(t -> t.startsWith("[상황]"))
                .anyMatch(t -> t.startsWith("[감정]"))
                .anyMatch(t -> t.startsWith("[사고]"));
    }

    @Test
    @DisplayName("3요소 모두 빈 본문 → saveAll 호출 안 함")
    void indexSummary_allBlank_noSave() {
        Summary s = Summary.builder()
                .id(102L).userId(1L).sessionId("s-3")
                .situationText(null).emotionText("").thoughtText("   ")
                .build();

        ragIndexingService.indexSummary(s);

        verify(ragChunkRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("summary == null / id == null — 가드 통과, saveAll 안 함")
    void indexSummary_guardOnNull() {
        ragIndexingService.indexSummary(null);
        ragIndexingService.indexSummary(Summary.builder().userId(1L).sessionId("s").build());
        verify(ragChunkRepository, never()).saveAll(any());
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}

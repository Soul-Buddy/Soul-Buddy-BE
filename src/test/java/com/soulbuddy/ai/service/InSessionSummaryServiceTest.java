package com.soulbuddy.ai.service;

import com.soulbuddy.ai.client.NaverSummaryClient;
import com.soulbuddy.domain.chat.entity.ChatMessage;
import com.soulbuddy.domain.chat.entity.ChatSessionRunningSummary;
import com.soulbuddy.domain.chat.repository.ChatMessageRepository;
import com.soulbuddy.domain.chat.repository.ChatSessionRunningSummaryRepository;
import com.soulbuddy.global.enums.Sender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PR-2 InSessionSummaryService 검증.
 *  - 80턴 미만 → 압축 안 함
 *  - 80턴 도달 → 가장 오래된 20개 압축, running_summary 신규 저장
 *  - 이미 압축 이력 있는 세션 → 두 번째 압축 시 기존 running_summary 에 concat
 *  - 네이버 요약 API 실패 → silently no-op
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InSessionSummaryServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatSessionRunningSummaryRepository runningSummaryRepository;
    @Mock private NaverSummaryClient naverSummaryClient;

    @InjectMocks
    private InSessionSummaryService service;

    private static final String SESSION_ID = "session-1";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "compactionThreshold", 80);
        ReflectionTestUtils.setField(service, "compactionBatchSize", 20);
    }

    @Test
    @DisplayName("미요약 메시지 79개 → 압축 안 함")
    void belowThreshold_noCompaction() {
        given(runningSummaryRepository.findBySessionId(SESSION_ID)).willReturn(Optional.empty());
        given(chatMessageRepository.countBySessionIdAndIdGreaterThan(SESSION_ID, 0L))
                .willReturn(79L);

        service.compactIfNeeded(SESSION_ID);

        verify(naverSummaryClient, never()).summarize(anyString());
        verify(runningSummaryRepository, never()).save(any());
    }

    @Test
    @DisplayName("미요약 80개 + 첫 압축 → batch 20개 요약, running_summary 신규 저장")
    void atThreshold_firstCompaction() {
        given(runningSummaryRepository.findBySessionId(SESSION_ID)).willReturn(Optional.empty());
        given(chatMessageRepository.countBySessionIdAndIdGreaterThan(SESSION_ID, 0L))
                .willReturn(80L);

        List<ChatMessage> batch = stubMessages(1, 20);
        given(chatMessageRepository.findBySessionIdAndIdGreaterThanOrderByIdAsc(
                eq(SESSION_ID), eq(0L), any(Pageable.class)))
                .willReturn(batch);
        given(naverSummaryClient.summarize(anyString())).willReturn("- 사용자가 시험 압박을 호소");

        service.compactIfNeeded(SESSION_ID);

        ArgumentCaptor<ChatSessionRunningSummary> captor =
                ArgumentCaptor.forClass(ChatSessionRunningSummary.class);
        verify(runningSummaryRepository).save(captor.capture());
        ChatSessionRunningSummary saved = captor.getValue();
        assertThat(saved.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(saved.getRunningSummary()).isEqualTo("- 사용자가 시험 압박을 호소");
        assertThat(saved.getLastCompactedMessageId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("두 번째 압축 → 기존 running_summary 에 새 요약 concat")
    void secondCompaction_concatExisting() {
        ChatSessionRunningSummary existing = ChatSessionRunningSummary.builder()
                .sessionId(SESSION_ID)
                .runningSummary("- 기존 요약")
                .lastCompactedMessageId(20L)
                .build();
        given(runningSummaryRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(existing));
        given(chatMessageRepository.countBySessionIdAndIdGreaterThan(SESSION_ID, 20L))
                .willReturn(80L);

        List<ChatMessage> batch = stubMessages(21, 40);
        given(chatMessageRepository.findBySessionIdAndIdGreaterThanOrderByIdAsc(
                eq(SESSION_ID), eq(20L), any(Pageable.class)))
                .willReturn(batch);
        given(naverSummaryClient.summarize(anyString())).willReturn("- 새 압축 결과");

        service.compactIfNeeded(SESSION_ID);

        assertThat(existing.getRunningSummary()).isEqualTo("- 기존 요약\n- 새 압축 결과");
        assertThat(existing.getLastCompactedMessageId()).isEqualTo(40L);
        // 이미 존재하므로 save 호출 안 함 (dirty checking 으로 업데이트)
        verify(runningSummaryRepository, never()).save(any());
    }

    @Test
    @DisplayName("네이버 요약 API 응답 null → silently no-op, 저장 안 함")
    void summaryApiFails_silentlyNoOp() {
        given(runningSummaryRepository.findBySessionId(SESSION_ID)).willReturn(Optional.empty());
        given(chatMessageRepository.countBySessionIdAndIdGreaterThan(SESSION_ID, 0L))
                .willReturn(100L);
        given(chatMessageRepository.findBySessionIdAndIdGreaterThanOrderByIdAsc(
                eq(SESSION_ID), anyLong(), any(Pageable.class)))
                .willReturn(stubMessages(1, 20));
        given(naverSummaryClient.summarize(anyString())).willReturn(null);

        service.compactIfNeeded(SESSION_ID);

        verify(runningSummaryRepository, never()).save(any());
    }

    private List<ChatMessage> stubMessages(int fromId, int toId) {
        return IntStream.rangeClosed(fromId, toId)
                .mapToObj(i -> ChatMessage.builder()
                        .id((long) i)
                        .sessionId(SESSION_ID)
                        .sender(i % 2 == 1 ? Sender.USER : Sender.ASSISTANT)
                        .content("메시지 " + i)
                        .build())
                .toList();
    }
}

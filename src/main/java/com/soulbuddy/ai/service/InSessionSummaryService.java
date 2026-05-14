package com.soulbuddy.ai.service;

import com.soulbuddy.ai.client.NaverSummaryClient;
import com.soulbuddy.domain.chat.entity.ChatMessage;
import com.soulbuddy.domain.chat.entity.ChatSessionRunningSummary;
import com.soulbuddy.domain.chat.repository.ChatMessageRepository;
import com.soulbuddy.domain.chat.repository.ChatSessionRunningSummaryRepository;
import com.soulbuddy.global.config.AsyncConfig;
import com.soulbuddy.global.enums.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 세션 내 슬라이딩 윈도우 압축 서비스 (PR-2 / v2.3 Hybrid Lite).
 *
 * 정책 (사용자 결정):
 *   ▣ 임계값: USER+ASSISTANT 합산 메시지 수 ≥ 80 일 때 압축 트리거
 *     - 정서 지원 대화 한 메시지 평균 200자 가정 시 약 24K 토큰 (HCX-005 32K 한도 안전 마진)
 *   ▣ 압축 단위: 가장 오래된 20개 메시지 → 한 텍스트로 join → 네이버 요약 API
 *   ▣ 보존: 최근 60개 messages 배열에 원문 (단기 기억)
 *   ▣ 누적: 두 번째 이상 압축은 기존 running_summary + 새 압축 결과 concat
 *   ▣ 비동기: @Async (aiBackgroundExecutor) — 채팅 응답 지연 0
 *
 * 호출 위치: ChatService.processChat 의 USER 메시지 저장 직후.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InSessionSummaryService {

    @Value("${soulbuddy.in-session-summary.threshold:80}")
    private int compactionThreshold;

    @Value("${soulbuddy.in-session-summary.batch-size:20}")
    private int compactionBatchSize;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRunningSummaryRepository runningSummaryRepository;
    private final NaverSummaryClient naverSummaryClient;

    /**
     * 압축 필요 시 가장 오래된 batch-size 개 메시지를 요약 API 로 압축.
     * 임계값 미달 / API 호출 실패 시 silently no-op.
     *
     * @param sessionId 세션 식별자
     */
    @Async(AsyncConfig.AI_BACKGROUND_EXECUTOR)
    @Transactional
    public void compactIfNeeded(String sessionId) {
        try {
            doCompact(sessionId);
        } catch (Exception e) {
            log.error("compactIfNeeded 실패 sessionId={} err={}", sessionId, e.getMessage(), e);
            // 압축 실패는 채팅 흐름에 영향 주지 않음. 다음 호출에서 다시 시도됨.
        }
    }

    private void doCompact(String sessionId) {
        Optional<ChatSessionRunningSummary> existingOpt = runningSummaryRepository.findBySessionId(sessionId);
        Long lastCompactedId = existingOpt.map(ChatSessionRunningSummary::getLastCompactedMessageId)
                .orElse(0L);

        long uncompactedCount = chatMessageRepository
                .countBySessionIdAndIdGreaterThan(sessionId, lastCompactedId);

        if (uncompactedCount < compactionThreshold) {
            return;
        }

        List<ChatMessage> batch = chatMessageRepository
                .findBySessionIdAndIdGreaterThanOrderByIdAsc(
                        sessionId, lastCompactedId, PageRequest.of(0, compactionBatchSize));

        if (batch.isEmpty()) {
            return;
        }

        String joined = joinForSummarization(batch);
        String newSummary = naverSummaryClient.summarize(joined);

        if (newSummary == null || newSummary.isBlank()) {
            log.warn("네이버 요약 API 응답 비어있음 sessionId={} batch size={}", sessionId, batch.size());
            return;
        }

        String mergedSummary = mergeWithExisting(
                existingOpt.map(ChatSessionRunningSummary::getRunningSummary).orElse(null),
                newSummary);

        Long newLastCompactedId = batch.get(batch.size() - 1).getId();

        if (existingOpt.isPresent()) {
            existingOpt.get().apply(mergedSummary, newLastCompactedId);
        } else {
            runningSummaryRepository.save(ChatSessionRunningSummary.builder()
                    .sessionId(sessionId)
                    .runningSummary(mergedSummary)
                    .lastCompactedMessageId(newLastCompactedId)
                    .build());
        }
        log.info("세션 내 압축 완료 sessionId={} compactedRange={}..{} 누적길이={}",
                sessionId, batch.get(0).getId(), newLastCompactedId, mergedSummary.length());
    }

    private String joinForSummarization(List<ChatMessage> batch) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : batch) {
            String role = (m.getSender() == Sender.USER) ? "사용자"
                    : (m.getSender() == Sender.ASSISTANT) ? "AI" : "시스템";
            sb.append(role).append(": ")
              .append(m.getContent() != null ? m.getContent() : "")
              .append('\n');
        }
        return sb.toString();
    }

    private String mergeWithExisting(String existing, String newSummary) {
        if (existing == null || existing.isBlank()) {
            return newSummary;
        }
        return existing + "\n" + newSummary;
    }
}

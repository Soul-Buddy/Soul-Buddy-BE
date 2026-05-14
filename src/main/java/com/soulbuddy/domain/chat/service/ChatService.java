package com.soulbuddy.domain.chat.service;

import com.soulbuddy.ai.client.NaverRagReasoningClient;
import com.soulbuddy.ai.dto.ChatRequest;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.ai.service.AiChatService;
import com.soulbuddy.ai.service.InSessionSummaryService;
import com.soulbuddy.domain.chat.dto.response.ChatHistoryResponse;
import com.soulbuddy.domain.chat.entity.ChatMessage;
import com.soulbuddy.domain.chat.entity.ChatSession;
import com.soulbuddy.domain.chat.entity.ChatSessionRunningSummary;
import com.soulbuddy.domain.chat.repository.ChatMessageRepository;
import com.soulbuddy.domain.chat.repository.ChatSessionRepository;
import com.soulbuddy.domain.chat.repository.ChatSessionRunningSummaryRepository;
import com.soulbuddy.domain.emotion.service.EmotionLogService;
import com.soulbuddy.domain.rag.service.RagSearchService;
import com.soulbuddy.domain.rag.service.RagTriggerDetector;
import com.soulbuddy.domain.summary.entity.Summary;
import com.soulbuddy.domain.summary.repository.SummaryRepository;
import com.soulbuddy.domain.user.service.ProfileQueryService;
import com.soulbuddy.global.enums.*;
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiChatService aiChatService;
    private final EmotionLogService emotionLogService;
    private final ProfileQueryService profileQueryService;
    private final com.soulbuddy.domain.safety.service.SafetyEventService safetyEventService;
    private final SummaryRepository summaryRepository;
    private final ChatSessionRunningSummaryRepository runningSummaryRepository;
    private final InSessionSummaryService inSessionSummaryService;
    private final RagTriggerDetector ragTriggerDetector;
    private final NaverRagReasoningClient naverRagReasoningClient;
    private final RagSearchService ragSearchService;

    @Value("${soulbuddy.rag.enabled:true}")
    private boolean ragEnabled;

    // POST /api/chat
    public ChatResponse processChat(Long userId, ChatRequest request) {

        ChatSession session = chatSessionRepository.findByIdAndUserId(request.getSessionId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_001));

        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.SESSION_002);
        }

        // v2.3 — 페르소나 세션 고정. 세션 시작 시 선택한 페르소나는 세션 종료까지 유지.
        // request 페르소나가 session 값과 다르면 session 값으로 강제(관대 처리, UX 끊김 방지).
        if (request.getPersonaType() != null && session.getPersonaType() != null
                && request.getPersonaType() != session.getPersonaType()) {
            log.warn("Persona mismatch — request={} session={}. session 값으로 보정.",
                    request.getPersonaType(), session.getPersonaType());
            request.setPersonaType(session.getPersonaType());
        }

        // v2.3 — recentSummary 자동 로드. request 가 null 일 때만 사용자의 가장 최근 세션
        // 요약(memoryHint)을 DB에서 자동 주입한다.
        if (request.getRecentSummary() == null || request.getRecentSummary().isBlank()) {
            summaryRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                    .map(Summary::getMemoryHint)
                    .filter(hint -> hint != null && !hint.isBlank())
                    .ifPresent(request::setRecentSummary);
        }

        // ① USER 메시지 저장 (분류 결과는 AI 호출 후 반영)
        ChatMessage userMessage = chatMessageRepository.save(
                ChatMessage.builder()
                        .sessionId(session.getId())
                        .sender(Sender.USER)
                        .content(request.getMessage())
                        .build()
        );

        // ② PromptContext 조립
        // PR-2 v2.3 — running_summary 로딩. 압축 이력 있는 세션은 마지막 미요약 구간만 messages 에 포함.
        Optional<ChatSessionRunningSummary> runningSummaryOpt =
                runningSummaryRepository.findBySessionId(session.getId());
        Long lastCompactedId = runningSummaryOpt
                .map(ChatSessionRunningSummary::getLastCompactedMessageId)
                .orElse(0L);
        String runningSummary = runningSummaryOpt
                .map(ChatSessionRunningSummary::getRunningSummary)
                .orElse(null);

        List<ChatMessage> recent = (lastCompactedId == null || lastCompactedId == 0L)
                ? chatMessageRepository.findTop20BySessionIdOrderByCreatedAtDesc(session.getId())
                : chatMessageRepository.findBySessionIdAndIdGreaterThanOrderByCreatedAtAsc(
                        session.getId(), lastCompactedId);
        if (lastCompactedId == null || lastCompactedId == 0L) {
            Collections.reverse(recent);
        }

        boolean firstTurn = recent.stream().filter(m -> m.getSender() == Sender.USER).count() == 1
                && (lastCompactedId == null || lastCompactedId == 0L);

        List<PromptContext.TurnMessage> recentTurns = recent.stream()
                .map(m -> PromptContext.TurnMessage.builder()
                        .role(m.getSender() == Sender.USER ? "user" : "assistant")
                        .content(m.getContent())
                        .build())
                .toList();

        // PR-6 — RAG: 사용자가 명시적으로 과거 대화를 회상하려는 발화일 때만 검색.
        //   ① RagTriggerDetector 키워드 매칭 → ② RAG Reasoning 으로 query 정제 →
        //   ③ RagSearchService FULLTEXT 검색 → Top-K 과거 세션 요약을 PromptContext.ragTop3 에 주입.
        // 검색 실패/결과 0건이면 ragTop3 = null 로 두어 user 메시지 d 블록 통째 생략.
        List<PromptContext.RagChunkRef> ragRefs = null;
        if (ragEnabled && ragTriggerDetector.isTriggered(request.getMessage())) {
            String refinedQuery = naverRagReasoningClient.extractSearchQuery(request.getMessage());
            if (refinedQuery != null && !refinedQuery.isBlank()) {
                List<PromptContext.RagChunkRef> found =
                        ragSearchService.searchTopK(userId, session.getId(), refinedQuery);
                if (!found.isEmpty()) ragRefs = found;
            }
        }

        PromptContext context = PromptContext.builder()
                .personaType(request.getPersonaType())
                .personalInstruction(profileQueryService.getPersonalInstructionByUserId(userId))
                .nickname(profileQueryService.getNicknameByUserId(userId))
                .recentSummary(request.getRecentSummary())
                .recentTurns(recentTurns)
                .runningSummary(runningSummary)
                .ragTop3(ragRefs)
                .firstTurn(firstTurn)
                .build();

        // PR-2 v2.3 — 80턴 도달 시 백그라운드 압축 트리거. 채팅 응답 흐름에 지연 0.
        inSessionSummaryService.compactIfNeeded(session.getId());

        // ③ recentHighCount 산출
        long recentHighCount = chatMessageRepository
                .countBySessionIdAndRiskLevel(session.getId(), RiskLevel.HIGH);

        // ④ AI 호출
        ChatResponse response = aiChatService.process(request, context, recentHighCount);

        // ⑤ ASSISTANT 메시지 저장
        ChatMessage assistantMessage = chatMessageRepository.save(
                ChatMessage.builder()
                        .sessionId(session.getId())
                        .sender(response.isForcedSafety() ? Sender.SYSTEM : Sender.ASSISTANT)
                        .content(response.getAssistantMessage())
                        .emotionTag(response.getEmotionTag())
                        .riskLevel(response.getRiskLevel())
                        .interventionType(response.getInterventionType())
                        .ragUsed(response.isRagUsed())
                        .aiModel(response.getAiModel())
                        .build()
        );

        // ⑥ 감정 로그
        if (response.getEmotionTag() != null) {
            emotionLogService.log(userId, session.getId(),
                    userMessage.getId(), response.getEmotionTag(), EmotionSource.MESSAGE);
        }

        // ⑦ 위험도 HIGH → safety 이벤트
        if (response.getRiskLevel() == RiskLevel.HIGH) {
            safetyEventService.recordRiskDetected(
                    userId,
                    session.getId(),
                    userMessage.getId(),
                    RiskLevel.HIGH,
                    false
            );
        }

        // ⑧ 강제 안전 응답 → safety 이벤트
        if (response.isForcedSafety()) {
            safetyEventService.recordForcedSafetyReply(
                    userId,
                    session.getId(),
                    assistantMessage.getId(),
                    response.getRiskLevel(),
                    true
            );
        }

        return response;
    }

    // GET /api/chat/history/{sessionId}
    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(Long userId, String sessionId, int page, int size) {

        chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_001));

        Page<ChatMessage> messagePage = chatMessageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId, PageRequest.of(page, size));

        List<ChatHistoryResponse.MessageItem> items = messagePage.getContent().stream()
                .map(m -> ChatHistoryResponse.MessageItem.builder()
                        .messageId(m.getId())
                        .sender(m.getSender())
                        .content(m.getContent())
                        .emotionTag(m.getEmotionTag())
                        .riskLevel(m.getRiskLevel())
                        .interventionType(m.getInterventionType())
                        .ragUsed(m.isRagUsed())
                        .aiModel(m.getAiModel())
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();

        return ChatHistoryResponse.builder()
                .sessionId(sessionId)
                .messages(items)
                .totalCount(messagePage.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }
}

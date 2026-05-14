package com.soulbuddy.domain.chat.service;

import com.soulbuddy.ai.client.NaverRagReasoningClient;
import com.soulbuddy.ai.dto.ChatRequest;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.ai.service.AiChatService;
import com.soulbuddy.ai.service.InSessionSummaryService;
import com.soulbuddy.domain.chat.entity.ChatMessage;
import com.soulbuddy.domain.chat.entity.ChatSession;
import com.soulbuddy.domain.chat.repository.ChatMessageRepository;
import com.soulbuddy.domain.chat.repository.ChatSessionRepository;
import com.soulbuddy.domain.chat.repository.ChatSessionRunningSummaryRepository;
import com.soulbuddy.domain.emotion.service.EmotionLogService;
import com.soulbuddy.domain.rag.service.RagSearchService;
import com.soulbuddy.domain.rag.service.RagTriggerDetector;
import com.soulbuddy.domain.safety.service.SafetyEventService;
import com.soulbuddy.domain.summary.entity.Summary;
import com.soulbuddy.domain.summary.repository.SummaryRepository;
import com.soulbuddy.domain.user.service.ProfileQueryService;
import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.PersonaType;
import com.soulbuddy.global.enums.RiskLevel;
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
import org.springframework.test.util.ReflectionTestUtils;

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
 * PR-1 ChatService 검증.
 *  - 페르소나 mismatch 시 session 값으로 보정
 *  - request.recentSummary == null 일 때 SummaryRepository 자동 로드 적중
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {

    @Mock private ChatSessionRepository chatSessionRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private AiChatService aiChatService;
    @Mock private EmotionLogService emotionLogService;
    @Mock private ProfileQueryService profileQueryService;
    @Mock private SafetyEventService safetyEventService;
    @Mock private SummaryRepository summaryRepository;
    @Mock private ChatSessionRunningSummaryRepository runningSummaryRepository;
    @Mock private InSessionSummaryService inSessionSummaryService;
    @Mock private RagTriggerDetector ragTriggerDetector;
    @Mock private NaverRagReasoningClient naverRagReasoningClient;
    @Mock private RagSearchService ragSearchService;

    @InjectMocks
    private ChatService chatService;

    private static final Long USER_ID = 42L;
    private static final String SESSION_ID = "session-1";

    @BeforeEach
    void setUp() {
        // PR-6 — 기본은 RAG 비활성화. RAG 케이스에서는 개별 테스트가 true 로 덮어쓴다.
        ReflectionTestUtils.setField(chatService, "ragEnabled", true);
        given(ragTriggerDetector.isTriggered(anyString())).willReturn(false);

        ChatMessage savedUserMsg = ChatMessage.builder()
                .sessionId(SESSION_ID).sender(Sender.USER).content("안녕").build();
        ChatMessage savedAssistantMsg = ChatMessage.builder()
                .sessionId(SESSION_ID).sender(Sender.ASSISTANT).content("응 안녕").build();
        given(chatMessageRepository.save(any(ChatMessage.class)))
                .willReturn(savedUserMsg).willReturn(savedAssistantMsg);

        given(chatMessageRepository.findTop20BySessionIdOrderByCreatedAtDesc(SESSION_ID))
                .willReturn(List.of(savedUserMsg));
        given(chatMessageRepository.countBySessionIdAndRiskLevel(eq(SESSION_ID), any()))
                .willReturn(0L);

        given(profileQueryService.getPersonalInstructionByUserId(USER_ID)).willReturn(null);
        given(profileQueryService.getNicknameByUserId(USER_ID)).willReturn("지훈");

        ChatResponse stub = ChatResponse.builder()
                .assistantMessage("응")
                .emotionTag(EmotionTag.HAPPY)
                .riskLevel(RiskLevel.LOW)
                .ragUsed(false)
                .forcedSafety(false)
                .build();
        given(aiChatService.process(any(), any(), anyLong())).willReturn(stub);
    }

    @Test
    @DisplayName("페르소나 mismatch — request 가 session 값으로 보정된다")
    void personaMismatch_forcedToSession() {
        ChatSession session = ChatSession.builder()
                .id(SESSION_ID).userId(USER_ID).personaType(PersonaType.COUNSELOR).build();
        given(chatSessionRepository.findByIdAndUserId(SESSION_ID, USER_ID))
                .willReturn(Optional.of(session));
        given(summaryRepository.findTopByUserIdOrderByCreatedAtDesc(USER_ID))
                .willReturn(Optional.empty());

        ChatRequest req = ChatRequest.builder()
                .sessionId(SESSION_ID)
                .personaType(PersonaType.FRIEND)  // session 은 COUNSELOR — mismatch
                .message("안녕")
                .recentSummary("이미 있음")
                .build();

        chatService.processChat(USER_ID, req);

        // request 의 personaType 이 session 값으로 보정되었는지
        assertThat(req.getPersonaType()).isEqualTo(PersonaType.COUNSELOR);

        ArgumentCaptor<ChatRequest> reqCaptor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(aiChatService).process(reqCaptor.capture(), any(), anyLong());
        assertThat(reqCaptor.getValue().getPersonaType()).isEqualTo(PersonaType.COUNSELOR);
    }

    @Test
    @DisplayName("recentSummary == null — SummaryRepository 자동 로드 적중")
    void recentSummary_autoloadFromSummaryRepository() {
        ChatSession session = ChatSession.builder()
                .id(SESSION_ID).userId(USER_ID).personaType(PersonaType.COUNSELOR).build();
        given(chatSessionRepository.findByIdAndUserId(SESSION_ID, USER_ID))
                .willReturn(Optional.of(session));

        Summary last = Summary.builder()
                .sessionId("prev-session").userId(USER_ID)
                .summaryText("이전 세션 요약")
                .memoryHint("[사실] 시험 압박 [감정] 불안")
                .build();
        given(summaryRepository.findTopByUserIdOrderByCreatedAtDesc(USER_ID))
                .willReturn(Optional.of(last));

        ChatRequest req = ChatRequest.builder()
                .sessionId(SESSION_ID)
                .personaType(PersonaType.COUNSELOR)
                .message("안녕")
                .recentSummary(null)  // 자동 로드 대상
                .build();

        chatService.processChat(USER_ID, req);

        assertThat(req.getRecentSummary()).isEqualTo("[사실] 시험 압박 [감정] 불안");
    }

    @Test
    @DisplayName("PR-6 RAG 트리거 hit → ragRefs 가 PromptContext 에 주입")
    void rag_triggered_injectsRefsIntoContext() {
        ChatSession session = ChatSession.builder()
                .id(SESSION_ID).userId(USER_ID).personaType(PersonaType.COUNSELOR).build();
        given(chatSessionRepository.findByIdAndUserId(SESSION_ID, USER_ID))
                .willReturn(Optional.of(session));

        given(ragTriggerDetector.isTriggered("저번에 학교 얘기했던 거 기억나?")).willReturn(true);
        given(naverRagReasoningClient.extractSearchQuery("저번에 학교 얘기했던 거 기억나?"))
                .willReturn("학교");
        PromptContext.RagChunkRef ref = PromptContext.RagChunkRef.builder()
                .date("2026-05-10").situation("학교에서 시험 압박").emotion("불안").thought("실패할 것 같다").build();
        given(ragSearchService.searchTopK(USER_ID, SESSION_ID, "학교"))
                .willReturn(List.of(ref));

        ChatRequest req = ChatRequest.builder()
                .sessionId(SESSION_ID)
                .personaType(PersonaType.COUNSELOR)
                .message("저번에 학교 얘기했던 거 기억나?")
                .recentSummary("recent")
                .build();

        chatService.processChat(USER_ID, req);

        ArgumentCaptor<PromptContext> ctxCaptor = ArgumentCaptor.forClass(PromptContext.class);
        verify(aiChatService).process(any(), ctxCaptor.capture(), anyLong());
        PromptContext captured = ctxCaptor.getValue();
        assertThat(captured.getRagTop3()).isNotNull().hasSize(1);
        assertThat(captured.getRagTop3().get(0).getSituation()).isEqualTo("학교에서 시험 압박");
    }

    @Test
    @DisplayName("PR-6 RAG 트리거 미발생 — ragTop3 = null, RAG client 호출 안 함")
    void rag_notTriggered_skipsAllRagCalls() {
        ChatSession session = ChatSession.builder()
                .id(SESSION_ID).userId(USER_ID).personaType(PersonaType.COUNSELOR).build();
        given(chatSessionRepository.findByIdAndUserId(SESSION_ID, USER_ID))
                .willReturn(Optional.of(session));

        // setUp 에서 isTriggered=false 기본값이므로 그대로

        ChatRequest req = ChatRequest.builder()
                .sessionId(SESSION_ID)
                .personaType(PersonaType.COUNSELOR)
                .message("그냥 오늘 기분이 우울해")
                .recentSummary("recent")
                .build();

        chatService.processChat(USER_ID, req);

        verify(naverRagReasoningClient, never()).extractSearchQuery(anyString());
        verify(ragSearchService, never()).searchTopK(anyLong(), anyString(), anyString());

        ArgumentCaptor<PromptContext> ctxCaptor = ArgumentCaptor.forClass(PromptContext.class);
        verify(aiChatService).process(any(), ctxCaptor.capture(), anyLong());
        assertThat(ctxCaptor.getValue().getRagTop3()).isNull();
    }

    @Test
    @DisplayName("recentSummary 이미 채워져 있으면 SummaryRepository 호출 안 함")
    void recentSummary_skipAutoloadWhenProvided() {
        ChatSession session = ChatSession.builder()
                .id(SESSION_ID).userId(USER_ID).personaType(PersonaType.COUNSELOR).build();
        given(chatSessionRepository.findByIdAndUserId(SESSION_ID, USER_ID))
                .willReturn(Optional.of(session));

        ChatRequest req = ChatRequest.builder()
                .sessionId(SESSION_ID)
                .personaType(PersonaType.COUNSELOR)
                .message("안녕")
                .recentSummary("FE가 보내준 요약")
                .build();

        chatService.processChat(USER_ID, req);

        verify(summaryRepository, never()).findTopByUserIdOrderByCreatedAtDesc(any());
        assertThat(req.getRecentSummary()).isEqualTo("FE가 보내준 요약");
    }
}

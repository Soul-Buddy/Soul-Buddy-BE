package com.soulbuddy.ai.service;

import com.soulbuddy.ai.client.ClassifierClient;
import com.soulbuddy.ai.client.PersonaLlmClient;
import com.soulbuddy.ai.dto.ChatRequest;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.ai.dto.ClassificationResult;
import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.ai.filter.SafetyFilter;
import com.soulbuddy.ai.parser.AiResponseParser;
import com.soulbuddy.ai.prompt.PromptBuilder;
import com.soulbuddy.domain.safety.service.SafetyEventService;
import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.InterventionType;
import com.soulbuddy.global.enums.PersonaType;
import com.soulbuddy.global.enums.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PR-1 AiChatServiceImpl 검증.
 *  - 같은 세션 forced 이벤트 이미 존재 → forced=false (일반 응답 진행)
 *  - HIGH 누적 3회 + 미발화 → forced=true (안전 발화로 응답 교체)
 *  - MEDIUM 응답에 안내 텍스트 미부착 (MEDIUM_RECOMMEND 폐기)
 */
@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock private ClassifierClient classifierClient;
    @Mock private PersonaLlmClient personaLlmClient;
    @Mock private PromptBuilder promptBuilder;
    @Mock private AiResponseParser aiResponseParser;
    @Mock private SafetyFilter safetyFilter;
    @Mock private SafetyEventService safetyEventService;

    @InjectMocks
    private AiChatServiceImpl aiChatService;

    private static final String SESSION_ID = "session-1";
    private ChatRequest request;
    private PromptContext context;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiChatService, "forcedSafetyThreshold", 3);
        ReflectionTestUtils.setField(aiChatService, "counselingCenterPath", "/counseling-center");
        request = ChatRequest.builder()
                .sessionId(SESSION_ID)
                .personaType(PersonaType.COUNSELOR)
                .message("힘들어요")
                .build();
        context = PromptContext.builder()
                .personaType(PersonaType.COUNSELOR)
                .build();
    }

    @Test
    @DisplayName("HIGH 누적 3회 + 강제 안전 발화 미발화 → forced=true")
    void forcedSafety_thresholdReached_andNotEmitted() {
        ClassificationResult classification = ClassificationResult.builder()
                .emotion(EmotionTag.HURT)
                .risk(RiskLevel.HIGH)
                .intervention(InterventionType.SYMPATHY_SUPPORT)
                .build();
        given(classifierClient.classifyParallel(anyString())).willReturn(classification);
        given(safetyFilter.decideForcedSafety(eq(RiskLevel.HIGH), eq(2L), eq(3))).willReturn(true);
        given(safetyEventService.hasForcedSafetyEmitted(SESSION_ID)).willReturn(false);
        given(aiResponseParser.safetyResponse()).willReturn(
                ChatResponse.builder()
                        .assistantMessage(SafetyFilter.SAFETY_MESSAGE)
                        .riskLevel(RiskLevel.HIGH)
                        .forcedSafety(true)
                        .recommendedAction(SafetyFilter.SAFETY_ACTION)
                        .build());

        ChatResponse response = aiChatService.process(request, context, 2L);

        assertThat(response.isForcedSafety()).isTrue();
        assertThat(response.getAssistantMessage()).isEqualTo(SafetyFilter.SAFETY_MESSAGE);
        // v2.3 FE 신호: showSafetyChoice / counselingCenterPath
        assertThat(response.isShowSafetyChoice()).isTrue();
        assertThat(response.getCounselingCenterPath()).isEqualTo("/counseling-center");
        // 페르소나 LLM 호출하지 않음
        verify(personaLlmClient, never()).call(any(), anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("HIGH 누적 3회 + 같은 세션에 이미 강제 안전 발화 이력 있음 → forced=false (일반 응답)")
    void forcedSafety_skippedWhenAlreadyEmitted() {
        ClassificationResult classification = ClassificationResult.builder()
                .emotion(EmotionTag.HURT)
                .risk(RiskLevel.HIGH)
                .intervention(InterventionType.SYMPATHY_SUPPORT)
                .build();
        given(classifierClient.classifyParallel(anyString())).willReturn(classification);
        given(safetyFilter.decideForcedSafety(eq(RiskLevel.HIGH), eq(2L), eq(3))).willReturn(true);
        given(safetyEventService.hasForcedSafetyEmitted(SESSION_ID)).willReturn(true);  // 이미 발화

        given(promptBuilder.build(any())).willReturn("system");
        given(promptBuilder.buildUserMessage(any(), anyString())).willReturn("user");
        given(promptBuilder.recentTurnsTruncated(any())).willReturn(null);
        given(personaLlmClient.call(any(), anyString(), anyString(), any())).willReturn("정상 페르소나 응답");
        given(aiResponseParser.sanitizeAssistantMessage("정상 페르소나 응답")).willReturn("정상 페르소나 응답");

        ChatResponse response = aiChatService.process(request, context, 2L);

        assertThat(response.isForcedSafety()).isFalse();
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.HIGH);  // 분류는 HIGH 그대로 기록
        assertThat(response.getAssistantMessage()).isEqualTo("정상 페르소나 응답");
        // v2.3 FE 신호: 일반 응답에서는 default
        assertThat(response.isShowSafetyChoice()).isFalse();
        assertThat(response.getCounselingCenterPath()).isNull();
        // 안전 발화 응답으로 교체하지 않음
        verify(aiResponseParser, never()).safetyResponse();
    }

    @Test
    @DisplayName("MEDIUM 응답에 안내 텍스트(MEDIUM_RECOMMEND) 미부착")
    void medium_noRecommendAppended() {
        ClassificationResult classification = ClassificationResult.builder()
                .emotion(EmotionTag.SAD)
                .risk(RiskLevel.MEDIUM)
                .intervention(InterventionType.SYMPATHY_SUPPORT)
                .build();
        given(classifierClient.classifyParallel(anyString())).willReturn(classification);
        given(safetyFilter.decideForcedSafety(eq(RiskLevel.MEDIUM), anyLong(), anyInt()))
                .willReturn(false);
        given(safetyEventService.hasForcedSafetyEmitted(SESSION_ID)).willReturn(false);

        given(promptBuilder.build(any())).willReturn("system");
        given(promptBuilder.buildUserMessage(any(), anyString())).willReturn("user");
        given(promptBuilder.recentTurnsTruncated(any())).willReturn(null);
        String personaReply = "지금 그 감정이 충분히 자연스럽다고 느껴요.";
        given(personaLlmClient.call(any(), anyString(), anyString(), any())).willReturn(personaReply);
        given(aiResponseParser.sanitizeAssistantMessage(personaReply)).willReturn(personaReply);

        ChatResponse response = aiChatService.process(request, context, 0L);

        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(response.getAssistantMessage()).isEqualTo(personaReply);
        // MEDIUM_RECOMMEND 안내 텍스트가 부착되지 않았는지
        assertThat(response.getAssistantMessage()).doesNotContain("전문 상담사와");
        assertThat(response.getAssistantMessage()).doesNotContain("혼자 견디지");
    }
}

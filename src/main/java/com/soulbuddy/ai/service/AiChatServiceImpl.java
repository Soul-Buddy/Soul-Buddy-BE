package com.soulbuddy.ai.service;

import com.soulbuddy.ai.client.ClassifierClient;
import com.soulbuddy.ai.client.PersonaLlmClient;
import com.soulbuddy.ai.dto.ChatRequest;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.ai.dto.ClassificationResult;
import com.soulbuddy.ai.dto.OpeningContext;
import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.ai.filter.SafetyFilter;
import com.soulbuddy.ai.parser.AiResponseParser;
import com.soulbuddy.ai.prompt.PromptBuilder;
import com.soulbuddy.domain.safety.service.SafetyEventService;
import com.soulbuddy.global.enums.RiskLevel;
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ClassifierClient classifierClient;
    private final PersonaLlmClient personaLlmClient;
    private final PromptBuilder promptBuilder;
    private final AiResponseParser aiResponseParser;
    private final SafetyFilter safetyFilter;
    private final SafetyEventService safetyEventService;

    @Value("${soulbuddy.safety.forced-safety-threshold:3}")
    private int forcedSafetyThreshold;

    @Value("${soulbuddy.safety.counseling-center-path:/counseling-center}")
    private String counselingCenterPath;

    @Override
    public ChatResponse process(ChatRequest request, PromptContext context, long recentHighCount) {
        long start = System.currentTimeMillis();

        // 분류 3종 병렬 (감정 / 위험도 / 개입유형)
        ClassificationResult classification = classifierClient.classifyParallel(request.getMessage());

        // v2.3 Safety Gate
        //  - immediateHighRisk(키워드 직격 차단) 정책 폐기. 분류기 HIGH 결과만 인정.
        //  - HIGH 누적 임계치(default 3) 도달 + 같은 세션에 강제 안전 발화 이력 없음 → forced.
        boolean thresholdReached =
                safetyFilter.decideForcedSafety(classification.getRisk(), recentHighCount, forcedSafetyThreshold);
        boolean alreadyEmitted =
                safetyEventService.hasForcedSafetyEmitted(request.getSessionId());
        boolean forced = thresholdReached && !alreadyEmitted;

        if (forced) {
            ChatResponse safety = aiResponseParser.safetyResponse();
            safety.setEmotionTag(classification.getEmotion());
            // v2.3 — FE 신호: 상담센터 이동 / 대화 이어가기 선택 UI 노출.
            safety.setShowSafetyChoice(true);
            safety.setCounselingCenterPath(counselingCenterPath);
            log.info("Forced safety reply triggered. classifiedRisk={} recentHighCount={} threshold={} ({}ms)",
                    classification.getRisk(), recentHighCount, forcedSafetyThreshold,
                    System.currentTimeMillis() - start);
            return safety;
        }

        PromptContext finalContext = PromptContext.builder()
                .personalInstruction(context.getPersonalInstruction())
                .nickname(context.getNickname())
                .personaType(context.getPersonaType())
                .recentSummary(context.getRecentSummary())
                .recentTurns(context.getRecentTurns())
                .classifiedEmotion(classification.getEmotion())
                .classifiedRisk(classification.getRisk())
                .classifiedIntervention(classification.getIntervention())
                .ragTop3(context.getRagTop3())
                .firstTurn(context.isFirstTurn())
                .openingMeta(context.getOpeningMeta())
                .build();

        String systemPrompt = promptBuilder.build(finalContext);
        String userMessage = promptBuilder.buildUserMessage(finalContext, request.getMessage());

        String raw = personaLlmClient.call(
                request.getPersonaType(),
                systemPrompt,
                userMessage,
                promptBuilder.recentTurnsTruncated(finalContext.getRecentTurns()));

        if (raw == null) {
            log.error("Persona LLM (HCX-005-{}) 호출 실패 — raw=null", request.getPersonaType());
            throw new BusinessException(ErrorCode.AI_001);
        }

        String assistantMessage = aiResponseParser.sanitizeAssistantMessage(raw);

        // v2.3 — MEDIUM_RECOMMEND 안내 텍스트 부착 정책 폐기.

        ChatResponse response = ChatResponse.builder()
                .assistantMessage(assistantMessage)
                .emotionTag(classification.getEmotion())
                .riskLevel(classification.getRisk())
                .interventionType(classification.getIntervention())
                .ragUsed(finalContext.getRagTop3() != null && !finalContext.getRagTop3().isEmpty())
                .aiModel(request.getPersonaType().aiModelTag())
                .forcedSafety(false)
                .summary(null)
                .memoryHint(null)
                .recommendedAction(classification.getRisk() == RiskLevel.HIGH
                        ? SafetyFilter.SAFETY_ACTION : null)
                .build();

        log.info("Persona LLM 응답 완료. persona={} risk={} ({}ms)",
                request.getPersonaType(), classification.getRisk(),
                System.currentTimeMillis() - start);
        return response;
    }

    @Override
    public String openingMessage(OpeningContext ctx) {
        long start = System.currentTimeMillis();

        PromptContext promptContext = PromptContext.builder()
                .nickname(ctx.getNickname())
                .personaType(ctx.getPersonaType())
                .recentSummary(ctx.getRecentSummary())
                .firstTurn(true)
                .openingMeta(PromptContext.OpeningMeta.builder()
                        .visitState(ctx.getVisitState().name())
                        .priorTopic(ctx.getPriorTopic())
                        .priorSeverity(ctx.getPriorSeverity() != null ? ctx.getPriorSeverity().name() : null)
                        .priorUnresolved(ctx.isPriorUnresolved())
                        .build())
                .build();

        String systemPrompt = promptBuilder.build(promptContext);

        String raw = personaLlmClient.call(
                ctx.getPersonaType(),
                systemPrompt,
                "(인사말을 자율 생성하세요)",
                Collections.emptyList());

        if (raw == null) {
            log.error("Opening LLM (HCX-005-{}) 호출 실패 — raw=null", ctx.getPersonaType());
            throw new BusinessException(ErrorCode.AI_001);
        }

        String message = aiResponseParser.sanitizeAssistantMessage(raw);
        log.info("Opening 생성 완료. persona={} visitState={} ({}ms)",
                ctx.getPersonaType(), ctx.getVisitState(), System.currentTimeMillis() - start);
        return message;
    }
}

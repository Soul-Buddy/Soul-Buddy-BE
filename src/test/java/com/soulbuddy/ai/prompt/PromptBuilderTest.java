package com.soulbuddy.ai.prompt;

import com.soulbuddy.ai.dto.PromptContext;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * PR-1 PromptBuilder v2.3 검증.
 *  - buildUserMessage 4블록 형식
 *  - ragTop3 비었을 때 [관련 과거 대화] 블록 통째 생략
 *  - system 메시지에 risk(B 위험도)/분류 메타 미포함
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromptBuilderTest {

    @Mock
    private SystemPromptLoader loader;

    @InjectMocks
    private PromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        given(loader.getCommonRules()).willReturn("공통 안전 규칙 본문");
        given(loader.getPersonaPrompt(any())).willReturn("페르소나 본문");
        given(loader.getInterventionPrompt(any())).willReturn("개입 유형 본문");
        given(loader.getOpeningGreeting()).willReturn("인사말 지침 본문");
    }

    @Test
    @DisplayName("buildUserMessage — 4블록 plain text 형식, RAG 비면 d 블록 생략")
    void buildUserMessage_4block_withoutRag() {
        PromptContext ctx = PromptContext.builder()
                .personaType(PersonaType.COUNSELOR)
                .classifiedEmotion(EmotionTag.ANXIOUS)
                .classifiedIntervention(InterventionType.SYMPATHY_SUPPORT)
                .classifiedRisk(RiskLevel.HIGH)
                .ragTop3(List.of())
                .build();

        String userMsg = promptBuilder.buildUserMessage(ctx, "오늘 발표 망쳤어 진짜 너무 속상해…");

        assertThat(userMsg).contains("[내담자 발화]");
        assertThat(userMsg).contains("오늘 발표 망쳤어 진짜 너무 속상해…");
        assertThat(userMsg).contains("[감정] anxious");
        assertThat(userMsg).contains("[개입유형] sympathy_support");
        assertThat(userMsg).doesNotContain("[관련 과거 대화]");

        // B(위험도)는 user 메시지에 절대 미포함
        assertThat(userMsg).doesNotContain("[위험도]");
        assertThat(userMsg).doesNotContain("HIGH");
        assertThat(userMsg).doesNotContain("risk");
    }

    @Test
    @DisplayName("buildUserMessage — ragTop3 채워지면 [관련 과거 대화] 블록 포함")
    void buildUserMessage_withRag() {
        PromptContext ctx = PromptContext.builder()
                .personaType(PersonaType.COUNSELOR)
                .classifiedEmotion(EmotionTag.SAD)
                .classifiedIntervention(InterventionType.CLARIFICATION_REFLECTION)
                .ragTop3(List.of(
                        PromptContext.RagChunkRef.builder()
                                .date("2026-05-10")
                                .situation("기말시험 압박이 컸음")
                                .emotion("불안")
                                .thought("나는 항상 부족해")
                                .build()))
                .build();

        String userMsg = promptBuilder.buildUserMessage(ctx, "또 비슷한 일이 있어");

        assertThat(userMsg).contains("[관련 과거 대화]");
        assertThat(userMsg).contains("2026-05-10");
        assertThat(userMsg).contains("기말시험 압박이 컸음");
        assertThat(userMsg).contains("나는 항상 부족해");
    }

    @Test
    @DisplayName("build(system) — 분류 메타/위험도/RAG 블록이 system 에 포함되지 않는다")
    void build_system_excludesMetaAndRisk() {
        PromptContext ctx = PromptContext.builder()
                .personaType(PersonaType.COUNSELOR)
                .nickname("지훈")
                .personalInstruction("차분한 톤 유지")
                .recentSummary("지난 세션에서 시험 압박을 이야기했음")
                .classifiedEmotion(EmotionTag.ANXIOUS)
                .classifiedRisk(RiskLevel.HIGH)
                .classifiedIntervention(InterventionType.SYMPATHY_SUPPORT)
                .ragTop3(List.of(
                        PromptContext.RagChunkRef.builder()
                                .date("2026-05-10").situation("s").emotion("e").thought("t").build()))
                .firstTurn(false)
                .build();

        String system = promptBuilder.build(ctx);

        // 정상 포함
        assertThat(system).contains("공통 안전 규칙 본문");
        assertThat(system).contains("페르소나 본문");
        assertThat(system).contains("[직전 세션 요약]");
        assertThat(system).contains("메타 누설 방지");

        // 분류 메타(블록명/risk/감정/개입유형)는 system 에 미포함
        assertThat(system).doesNotContain("[내부 메타");
        assertThat(system).doesNotContain("분류된 위험도");
        assertThat(system).doesNotContain("HIGH");
        assertThat(system).doesNotContain("ANXIOUS");
        assertThat(system).doesNotContain("sympathy_support");

        // RAG 블록 system 미포함 (RAG는 user 메시지로 이동)
        assertThat(system).doesNotContain("[관련 과거 대화]");
    }
}

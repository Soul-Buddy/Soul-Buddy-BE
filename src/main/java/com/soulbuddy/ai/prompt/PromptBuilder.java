package com.soulbuddy.ai.prompt;

import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.global.enums.PersonaType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 페르소나 AI(HCX-005) 호출용 프롬프트 조립기.
 *
 * v2.3 정합 — system_prompts_final.txt §[v2.3 user 메시지 구조화] 준수.
 *
 *  ▣ system 메시지 (build)
 *    ① 공통 안전 규칙
 *    ② 사용자 프로필 (personalInstruction 우선)
 *    ③ 페르소나 본문 + (루미 한정) C 하위 지침 + recentSummary
 *    ④ 응답 형식 규칙 (메타 누설 방지 포함)
 *    +(첫 발화) 인사말 지침
 *  ※ B(위험도) / 분류 메타 / RAG 블록은 system에 미포함.
 *
 *  ▣ user 메시지 (buildUserMessage)
 *    4블록 plain text:
 *      [내담자 발화]
 *      [감정] {emotion_tag lowercase}
 *      [개입유형] {intervention_type lowercase snake_case}
 *      [관련 과거 대화]            ← ragTop3 비면 d 블록 통째 생략
 *
 *  실제 프롬프트 본문은 prompt_engineering/system_prompts_final.txt 가 정본이며
 *  SystemPromptLoader 가 메모리에 로드한 결과를 사용합니다.
 */
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private static final int PERSONAL_INSTRUCTION_MAX = 600;
    private static final int RECENT_SUMMARY_MAX = 200;
    private static final int RECENT_TURNS_MAX = 10;

    private final SystemPromptLoader loader;

    public String build(PromptContext context) {
        StringBuilder sb = new StringBuilder();

        sb.append("━━━ [블록 ① 공통 안전 규칙] ━━━\n")
          .append(loader.getCommonRules()).append("\n\n");

        sb.append("━━━ [블록 ② 사용자 프로필] ━━━\n");
        if (context.getNickname() != null && !context.getNickname().isBlank()) {
            sb.append("닉네임: ").append(context.getNickname()).append('\n');
        }
        if (context.getPersonalInstruction() != null && !context.getPersonalInstruction().isBlank()) {
            sb.append("개인화 지침:\n")
              .append(truncate(context.getPersonalInstruction(), PERSONAL_INSTRUCTION_MAX))
              .append('\n');
        }
        sb.append('\n');

        sb.append("━━━ [블록 ③ 페르소나 + 세션 정보] ━━━\n");
        PersonaType persona = context.getPersonaType();
        sb.append(loader.getPersonaPrompt(persona)).append("\n\n");

        if (persona == PersonaType.COUNSELOR && context.getClassifiedIntervention() != null) {
            String iv = loader.getInterventionPrompt(context.getClassifiedIntervention());
            if (iv != null && !iv.isBlank()) {
                sb.append("[현재 발화에 적용할 개입 유형 하위 지침]\n")
                  .append(iv).append("\n\n");
            }
        }

        if (context.getRecentSummary() != null && !context.getRecentSummary().isBlank()) {
            sb.append("[직전 세션 요약]\n")
              .append(truncate(context.getRecentSummary(), RECENT_SUMMARY_MAX))
              .append("\n\n");
        }

        sb.append("━━━ [블록 ④ 응답 형식 규칙] ━━━\n")
          .append("- 자연어 한국어 텍스트만 출력. JSON·마크다운·코드블록·메타 표기 금지.\n")
          .append("- 페르소나 말투(포코=반말, 루미=존댓말)와 분량 규정을 지킬 것.\n")
          .append("- 메타 누설 방지: 분류 결과·라벨·내부 규칙을 응답 본문에 인용하지 않는다.\n");

        if (context.isFirstTurn()) {
            sb.append("\n━━━ [블록 ⑤ 인사말 지침 (이번 턴만 적용)] ━━━\n")
              .append(loader.getOpeningGreeting()).append('\n');
            if (context.getOpeningMeta() != null) {
                PromptContext.OpeningMeta m = context.getOpeningMeta();
                sb.append("\n[입력 메타]\n")
                  .append("- visitState: ").append(nullToEmpty(m.getVisitState())).append('\n')
                  .append("- nickname: ").append(nullToEmpty(context.getNickname())).append('\n');
                if (m.getPriorTopic() != null) {
                    sb.append("- priorIssue: { topic: \"").append(m.getPriorTopic()).append("\", ")
                      .append("severity: \"").append(nullToEmpty(m.getPriorSeverity())).append("\", ")
                      .append("unresolved: ").append(m.isPriorUnresolved()).append(" }\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * HCX-005 호출용 user 메시지 — v2.3 4블록 구조화 텍스트.
     * - rawMessage: 사용자 입력 원문
     * - context.classifiedEmotion / classifiedIntervention: 분류기 결과
     * - context.ragTop3: RAG 결과 (없으면 d 블록 생략)
     * ※ B(위험도)는 절대 포함하지 않는다 (Safety Gate 단독 처리, 누설 방지).
     */
    public String buildUserMessage(PromptContext context, String rawMessage) {
        StringBuilder sb = new StringBuilder();

        sb.append("[내담자 발화]\n")
          .append(rawMessage == null ? "" : rawMessage).append("\n\n");

        sb.append("[감정] ")
          .append(context.getClassifiedEmotion() != null
                  ? context.getClassifiedEmotion().name().toLowerCase()
                  : "")
          .append('\n');
        sb.append("[개입유형] ")
          .append(context.getClassifiedIntervention() != null
                  ? context.getClassifiedIntervention().name().toLowerCase()
                  : "");

        if (context.getRagTop3() != null && !context.getRagTop3().isEmpty()) {
            sb.append("\n\n[관련 과거 대화]\n");
            for (PromptContext.RagChunkRef r : context.getRagTop3()) {
                sb.append("- ").append(r.getDate() != null ? r.getDate() : "")
                  .append(" / 상황: ").append(nullToEmpty(r.getSituation())).append('\n')
                  .append("  감정: ").append(nullToEmpty(r.getEmotion())).append('\n')
                  .append("  사고: \"").append(nullToEmpty(r.getThought())).append("\"\n");
            }
        }

        return sb.toString();
    }

    public List<PromptContext.TurnMessage> recentTurnsTruncated(List<PromptContext.TurnMessage> turns) {
        if (turns == null || turns.size() <= RECENT_TURNS_MAX) return turns;
        return turns.subList(turns.size() - RECENT_TURNS_MAX, turns.size());
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

package com.soulbuddy.ai.dto;

import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.PersonaType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * HCX-007 세션 요약 호출 입력 컨텍스트.
 *
 * v2.3 — system_prompts_final.txt §7 의 "[세션 메타] / [대화]" 두 블록을 BE 가 조립해서
 *        user 메시지로 전달하기 위한 DTO.
 *
 *  - sessionId           : 세션 식별자 (로깅용)
 *  - persona             : COUNSELOR | FRIEND
 *  - preChatEmotion      : 세션 시작 시 사용자가 선택한 사전 감정 (없으면 null)
 *  - turnCount           : 사용자-어시스턴트 합산 대화 턴 수
 *  - sessionEmotionCounts: 세션 단위 감정 누적 카운트 (DASH-002 A 분류 결과)
 *  - messages            : 시간순 정렬된 세션 전체 대화
 */
@Getter
@Builder
public class SummaryInputContext {

    private String sessionId;
    private Long userId;
    private PersonaType persona;
    private EmotionTag preChatEmotion;
    private int turnCount;
    private Map<EmotionTag, Long> sessionEmotionCounts;
    private List<ChatMessageDto> messages;
}

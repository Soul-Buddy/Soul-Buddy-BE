package com.soulbuddy.ai.service;

import com.soulbuddy.ai.dto.SummaryInputContext;
import com.soulbuddy.ai.dto.SummaryResult;

public interface AiSummaryService {

    /**
     * 세션 종료 시 HCX-007 요약 호출.
     * v2.3 — SummaryInputContext 로 세션 메타 + 대화를 묶어 전달.
     *        BE 에서 system_prompts_final.txt §7 입력 포맷대로 user 메시지를 조립한다.
     */
    SummaryResult summarize(SummaryInputContext context);
}

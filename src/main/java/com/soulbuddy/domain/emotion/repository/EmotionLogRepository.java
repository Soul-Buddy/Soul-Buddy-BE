package com.soulbuddy.domain.emotion.repository;

import com.soulbuddy.domain.emotion.entity.EmotionLog;
import com.soulbuddy.global.enums.EmotionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmotionLogRepository extends JpaRepository<EmotionLog, Long> {

    // GET /api/dashboard/me - 감정 태그별 카운트 집계
    @Query("SELECT e.emotionTag AS emotionTag, COUNT(e) AS count FROM EmotionLog e WHERE e.userId = :userId GROUP BY e.emotionTag")
    List<EmotionTagCount> countEmotionTagByUserId(@Param("userId") Long userId);

    /**
     * 세션 단위 감정 누적 카운트.
     * HCX-007 요약 호출 시 [세션 메타].sessionEmotionCounts 채우기용 (v2.3).
     */
    @Query("SELECT e.emotionTag AS emotionTag, COUNT(e) AS count FROM EmotionLog e " +
            "WHERE e.sessionId = :sessionId GROUP BY e.emotionTag")
    List<EmotionTagCount> countEmotionTagBySessionId(@Param("sessionId") String sessionId);

    interface EmotionTagCount {
        EmotionTag getEmotionTag();
        Long getCount();
    }
}

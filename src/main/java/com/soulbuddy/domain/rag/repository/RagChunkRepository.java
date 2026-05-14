package com.soulbuddy.domain.rag.repository;

import com.soulbuddy.domain.rag.entity.RagChunk;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RagChunkRepository extends JpaRepository<RagChunk, Long> {

    List<RagChunk> findByUserId(Long userId);

    List<RagChunk> findBySessionId(String sessionId);

    /**
     * PR-6 — 같은 user_id 의 과거 청크 중 FULLTEXT(MATCH) 점수 상위 N개를 반환.
     *
     * - ngram parser + BOOLEAN MODE: 한국어 부분 매칭. 검색어를 그대로 넣으면
     *   ngram 토큰 분해 후 매칭.
     * - 같은 세션의 청크가 검색 결과에 끼면 "방금 한 얘기를 인용" 하는 모양이 되어
     *   메타 누설처럼 어색하므로 currentSessionId 와 다른 세션만 반환.
     * - score 동률 시 indexed_at DESC 로 최근 세션 우선.
     */
    @Query(value = """
            SELECT *
            FROM rag_chunks
            WHERE user_id = :userId
              AND session_id <> :currentSessionId
              AND MATCH(chunk_text, pulling_text) AGAINST (:query IN BOOLEAN MODE) > 0
            ORDER BY MATCH(chunk_text, pulling_text) AGAINST (:query IN BOOLEAN MODE) DESC,
                     indexed_at DESC
            """, nativeQuery = true)
    List<RagChunk> searchFullText(
            @Param("userId") Long userId,
            @Param("currentSessionId") String currentSessionId,
            @Param("query") String query,
            Pageable pageable
    );
}

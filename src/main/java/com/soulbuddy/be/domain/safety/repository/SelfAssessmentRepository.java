package com.soulbuddy.be.domain.safety.repository;

import com.soulbuddy.be.domain.safety.entity.SelfAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SelfAssessmentRepository extends JpaRepository<SelfAssessment, Long> {

    List<SelfAssessment> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<SelfAssessment> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    List<SelfAssessment> findBySessionId(String sessionId);
}

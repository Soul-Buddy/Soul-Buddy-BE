package com.soulbuddy.domain.counseling.repository;

import com.soulbuddy.domain.counseling.entity.CounselingCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface Counselingcenterrepository extends JpaRepository<CounselingCenter, Long> {

    @Query("SELECT c FROM CounselingCenter c " +
            "WHERE (:region IS NULL OR c.region = :region) " +
            "AND (:emergencyOnly IS FALSE OR c.isEmergency = :emergencyOnly) " +
            "ORDER BY c.isEmergency DESC, c.name ASC")
    List<CounselingCenter> findCentersWithFilter(@Param("region") String region,
                                                 @Param("emergencyOnly") Boolean emergencyOnly);
}
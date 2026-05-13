package com.soulbuddy.domain.counseling.repository;

import com.soulbuddy.domain.counseling.entity.CounselingCenter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Counselingcenterrepository extends JpaRepository<CounselingCenter, Long> {

    List<CounselingCenter> findAllByOrderByIsEmergencyDescNameAsc();
    List<CounselingCenter> findByRegionOrderByIsEmergencyDescNameAsc(String region);
    List<CounselingCenter> findByIsEmergencyTrueOrderByNameAsc();
    List<CounselingCenter> findByRegionAndIsEmergencyTrueOrderByNameAsc(String region);
}
package com.soulbuddy.domain.counseling.service;

import com.soulbuddy.domain.counseling.dto.Counselingcenterlistdto;
import com.soulbuddy.domain.counseling.entity.CounselingCenter;
import com.soulbuddy.domain.counseling.repository.Counselingcenterrepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Counselingcenterservice {

    private final Counselingcenterrepository repository;

    @Transactional(readOnly = true)
    public List<Counselingcenterlistdto> getCenters(String region, Boolean emergencyOnly) {
        boolean hasRegion = region != null && !region.isBlank();
        boolean isEmergencyOnly = Boolean.TRUE.equals(emergencyOnly);

        List<CounselingCenter> centers;
        if (hasRegion && isEmergencyOnly) {
            centers = repository.findByRegionAndIsEmergencyTrueOrderByNameAsc(region);
        } else if (hasRegion) {
            centers = repository.findByRegionOrderByIsEmergencyDescNameAsc(region);
        } else if (isEmergencyOnly) {
            centers = repository.findByIsEmergencyTrueOrderByNameAsc();
        } else {
            centers = repository.findAllByOrderByIsEmergencyDescNameAsc();
        }

        return centers.stream()
                .map(Counselingcenterlistdto::fromEntity)
                .collect(Collectors.toList());
    }
}
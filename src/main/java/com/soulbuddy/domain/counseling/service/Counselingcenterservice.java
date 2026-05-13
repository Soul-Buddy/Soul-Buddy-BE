package com.soulbuddy.domain.counseling.service;

import com.soulbuddy.domain.counseling.dto.Counselingcenterlistdto;
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
        // Repository 쿼리 결과를 DTO로 변환
        return repository.findCentersWithFilter(region, emergencyOnly).stream()
                .map(Counselingcenterlistdto::fromEntity)
                .collect(Collectors.toList());
    }
}
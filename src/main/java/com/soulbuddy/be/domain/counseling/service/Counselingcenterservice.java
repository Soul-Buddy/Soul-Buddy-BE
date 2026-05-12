package com.soulbuddy.be.domain.counseling.service;

import com.soulbuddy.be.domain.counseling.dto.Counselingcenterrequestdto;
import com.soulbuddy.be.domain.counseling.dto.Counselingcenterresponsedto;
import com.soulbuddy.be.domain.counseling.dto.Counselingcenterlistdto;
import com.soulbuddy.be.domain.counseling.entity.Counselingcenter;
import com.soulbuddy.be.domain.counseling.repository.Counselingcenterrepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class Counselingcenterservice {

    private final Counselingcenterrepository counselingcenterrepository;

    public List<Counselingcenterlistdto> getAllActiveCenters() {
        return counselingcenterrepository.findAllActiveOrderByEmergencyAndSort()
                .stream()
                .map(this::convertToListDto)
                .collect(Collectors.toList());
    }

    public List<Counselingcenterlistdto> getCentersByRegion(String region) {
        return counselingcenterrepository.findByRegionAndActive(region)
                .stream()
                .map(this::convertToListDto)
                .collect(Collectors.toList());
    }

    public List<Counselingcenterlistdto> getEmergencyCenters() {
        return counselingcenterrepository.findAllEmergency()
                .stream()
                .map(this::convertToListDto)
                .collect(Collectors.toList());
    }

    public List<Counselingcenterlistdto> searchCentersByName(String name) {
        return counselingcenterrepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::convertToListDto)
                .collect(Collectors.toList());
    }

    public Counselingcenterresponsedto getCenterDetail(Long id) {
        Counselingcenter center = counselingcenterrepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상담센터를 찾을 수 없습니다. ID: " + id));
        return convertToResponseDto(center);
    }

    @Transactional
    public Counselingcenterresponsedto createCenter(Counselingcenterrequestdto requestDto) {
        Counselingcenter center = Counselingcenter.builder()
                .name(requestDto.getName())
                .phone(requestDto.getPhone())
                .region(requestDto.getRegion())
                .isEmergency(requestDto.getIsEmergency() != null ? requestDto.getIsEmergency() : true)
                .isActive(requestDto.getIsActive() != null ? requestDto.getIsActive() : true)
                .sortOrder(requestDto.getSortOrder() != null ? requestDto.getSortOrder() : 0)
                .createdAt(LocalDateTime.now())
                .build();

        Counselingcenter savedCenter = counselingcenterrepository.save(center);
        return convertToResponseDto(savedCenter);
    }

    @Transactional
    public Counselingcenterresponsedto updateCenter(Long id, Counselingcenterrequestdto requestDto) {
        Counselingcenter center = counselingcenterrepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상담센터를 찾을 수 없습니다. ID: " + id));

        if (requestDto.getName() != null) {
            center.setName(requestDto.getName());
        }
        if (requestDto.getPhone() != null) {
            center.setPhone(requestDto.getPhone());
        }
        if (requestDto.getRegion() != null) {
            center.setRegion(requestDto.getRegion());
        }
        if (requestDto.getIsEmergency() != null) {
            center.setIsEmergency(requestDto.getIsEmergency());
        }
        if (requestDto.getIsActive() != null) {
            center.setIsActive(requestDto.getIsActive());
        }
        if (requestDto.getSortOrder() != null) {
            center.setSortOrder(requestDto.getSortOrder());
        }
        center.setUpdatedAt(LocalDateTime.now());

        Counselingcenter updatedCenter = counselingcenterrepository.save(center);
        return convertToResponseDto(updatedCenter);
    }

    @Transactional
    public void deleteCenter(Long id) {
        if (!counselingcenterrepository.existsById(id)) {
            throw new RuntimeException("상담센터를 찾을 수 없습니다. ID: " + id);
        }
        counselingcenterrepository.deleteById(id);
    }

    @Transactional
    public Counselingcenterresponsedto deactivateCenter(Long id) {
        Counselingcenter center = counselingcenterrepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상담센터를 찾을 수 없습니다. ID: " + id));
        center.setIsActive(false);
        center.setUpdatedAt(LocalDateTime.now());
        Counselingcenter updatedCenter = counselingcenterrepository.save(center);
        return convertToResponseDto(updatedCenter);
    }

    private Counselingcenterresponsedto convertToResponseDto(Counselingcenter center) {
        return Counselingcenterresponsedto.builder()
                .id(center.getId())
                .name(center.getName())
                .phone(center.getPhone())
                .region(center.getRegion())
                .isEmergency(center.getIsEmergency())
                .isActive(center.getIsActive())
                .sortOrder(center.getSortOrder())
                .createdAt(center.getCreatedAt())
                .updatedAt(center.getUpdatedAt())
                .build();
    }

    private Counselingcenterlistdto convertToListDto(Counselingcenter center) {
        return Counselingcenterlistdto.builder()
                .id(center.getId())
                .name(center.getName())
                .phone(center.getPhone())
                .region(center.getRegion())
                .isEmergency(center.getIsEmergency())
                .sortOrder(center.getSortOrder())
                .createdAt(center.getCreatedAt())
                .build();
    }
}
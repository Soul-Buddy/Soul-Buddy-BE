package com.soulbuddy.domain.counseling.dto;

import com.soulbuddy.domain.counseling.entity.CounselingCenter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "상담센터 상세 정보 응답 DTO")
public class Counselingcenterresponsedto {

    @Schema(description = "센터 ID", example = "101")
    private Long id;

    @Schema(description = "센터명", example = "청소년 상담센터")
    private String name;

    @Schema(description = "전화번호", example = "02-1234-5678")
    private String phone;

    @Schema(description = "지역", example = "서울")
    private String region;

    @Schema(description = "24시간 긴급 상담 센터 여부", example = "true")
    private Boolean isEmergency;

    @Schema(description = "센터 활성화 여부", example = "true")
    private Boolean isActive;

    @Schema(description = "정렬 순서", example = "1")
    private Integer sortOrder;

    @Schema(description = "생성 일시", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 일시", example = "2024-01-15T14:30:00")
    private LocalDateTime updatedAt;

    public static Counselingcenterresponsedto fromEntity(CounselingCenter entity) {
        return Counselingcenterresponsedto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .phone(entity.getPhone())
                .region(entity.getRegion())
                .isEmergency(entity.getIsEmergency())
                .isActive(entity.getIsActive())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
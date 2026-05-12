package com.soulbuddy.be.domain.counseling.dto;

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
@Schema(description = "상담센터 목록 조회 응답 DTO")
public class Counselingcenterlistdto {

    @Schema(description = "센터 ID", example = "101")
    private Long id;

    @Schema(description = "센터명", example = "청소년 상담센터")
    private String name;

    @Schema(description = "전화번호", example = "02-1234-5678")
    private String phone;

    @Schema(description = "지역", example = "서울")
    private String region;

    @Schema(description = "긴급 센터 여부", example = "true")
    private Boolean isEmergency;

    @Schema(description = "정렬 순서", example = "1")
    private Integer sortOrder;

    @Schema(description = "생성 일시", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;
}
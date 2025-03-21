package com.kware.hybrid.service.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ResourcePodUsageVO {
    private String collectDt; // TO_CHAR 변환 적용 (String 형태로 저장)
    private String mlId;
    private String podUid;
    private BigDecimal usageCpu;
    private BigDecimal usageMemory;
    private BigDecimal usageNetwork;
    private String usageGpu; // JSON -> TEXT로 변환된 필드
    private String statusPhase;
}

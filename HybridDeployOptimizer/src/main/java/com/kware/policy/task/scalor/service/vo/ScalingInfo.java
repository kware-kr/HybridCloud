package com.kware.policy.task.scalor.service.vo;


import java.time.LocalDateTime;

import com.kware.common.openapi.vo.APIPagedRequest;

import lombok.Data;

@Data
public class ScalingInfo extends APIPagedRequest{
    private Long uid;             // 각 스케일링 기록의 고유 식별자
    private String scalingType;   // 스케일링 유형 (POD, NODE)
    private String docType;       // 문서 유형 (REQUEST, RESPONSE)
    private String docBody;       // 요청 또는 응답 본문의 JSON 데이터 (jsonb -> String)
    private String docDesc;       // 설명 (jsonb -> String)
    private LocalDateTime regDt;  // 등록 시각
}

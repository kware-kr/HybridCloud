package com.kware.policy.task.collector.service.vo;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kware.common.openapi.vo.APIPagedRequest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Db에서 특정기간의  사용정보를 조회한 결과,    
 */
@Getter
@Setter
public class ResourceUsageNode extends APIPagedRequest{
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Schema(type = "string", pattern = "yyyy-MM-dd HH:mm:ss", example = "2024-10-29 15:30:00")
    private Timestamp collectDt;
    private Integer clUid;
    private String nodeNm;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(type = "string", pattern = "yyyy-MM-dd HH:mm:ss", example = "2024-10-29 15:30:00")
    private Timestamp regDt;
    
    private String results; //jsonb를 String으로 변환
}
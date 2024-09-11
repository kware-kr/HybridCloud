package com.kware.policy.task.collector.service.vo;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kware.common.openapi.vo.APIPagedRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceUsagePod extends APIPagedRequest{
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp collectDt;
    private Integer clUid;
    private String mlId;
    private String podUid;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp regDt;
    
    private String results; // JSONB type mapped to String
}
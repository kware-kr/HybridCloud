package com.kware.policy.task.feature.service.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
public class ClusterNodeFeature {
	@JsonIgnore
	private Integer clUid;
	@JsonIgnore
	private String nodeName;
	
	private Integer gpuLevel;
    private Integer securityLevel;
    private Integer performanceLevel;
}

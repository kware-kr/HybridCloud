package com.kware.policy.task.feature.service.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
public class ClusterFeature {
	@JsonIgnore
	private int clUid;
	private String cloudType;     // 클라우드 구분 (PRI, PUB, ONP)
}

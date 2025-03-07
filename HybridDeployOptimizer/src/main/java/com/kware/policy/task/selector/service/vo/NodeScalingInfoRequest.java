package com.kware.policy.task.selector.service.vo;

import lombok.Data;

@Data
public class NodeScalingInfoRequest {
	Integer clusterId;
	String  nodeType; //gpu, normal
	Integer nodeCount;
	String  reason;
	String  callbackUrl;
}
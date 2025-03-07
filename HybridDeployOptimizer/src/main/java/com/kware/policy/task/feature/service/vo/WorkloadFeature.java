package com.kware.policy.task.feature.service.vo;

import lombok.Data;

@Data
public class WorkloadFeature {
    private int id;
    private String name;
    private int gpuLevel;
    private int priority;
    private String cloudType;
    private int nodeLevel;
    private String workloadType;
    private String priorityClass;
    private int securityLevel;
    private String deploymentStage;
    private String preemptionPolicy;
    
	
	public String toString2() {
		return "[name=" + name + ", gpuLevel=" + gpuLevel + ", priority=" + priority + ", cloudType="
				+ cloudType + ", nodeLevel=" + nodeLevel + ", workloadType=" + workloadType + ", priorityClass="
				+ priorityClass + ", securityLevel=" + securityLevel + ", deploymentStage=" + deploymentStage
				+ ", preemptionPolicy=" + preemptionPolicy + "]";
	}

    // 기본 생성자, 모든 필드를 포함한 생성자, Getter & Setter 추가 가능
}

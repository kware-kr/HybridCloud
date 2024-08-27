package com.kware.policy.task.collector.service.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@ToString
public class ClusterWorkloadPod extends ClusterDefault {
	private String uid;  // pod uid
	private String kind; // kind 이름
	private String pod;  // pod이름
	private String node; // 노드이름
	private String mlId; // ml이름
	private Integer clUid;
	private long   createdAt;
	private long   updatedAt;
	
	private String namespace;
	private String ownerUid;
	private Integer restart;
	private String ownerKind;
	
	private String status;
	
	private boolean isCompleted;

	@JsonIgnore
	@Override
	public String getUniqueKey() {
		return uid;
	}
}
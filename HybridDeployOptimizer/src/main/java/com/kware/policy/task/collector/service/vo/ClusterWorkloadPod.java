package com.kware.policy.task.collector.service.vo;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ClusterWorkloadPod extends ClusterDefault {
	private String uid;  // pod uid
	private String kind; // kind 이름
	private String pod;  // pod이름 name
	private String node; // 노드이름
	private String mlId; // ml이름
	private Integer clUid;
	
	//private long   createdAt;
	//private long   updatedAt;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp   createdAt;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp   updatedAt;
	
	private String namespace;
	private String ownerUid; // 이것 프로메테우스에서 찾을 수 없네.
	private String ownerName;
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
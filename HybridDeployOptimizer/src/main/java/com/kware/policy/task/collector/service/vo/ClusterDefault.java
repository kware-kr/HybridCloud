package com.kware.policy.task.collector.service.vo;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class ClusterDefault {
	private String deleteAt = "N";
	private Long regUid;
	private Timestamp regDt;
	private Long updtUid;
	private Timestamp updtDt;

	@JsonIgnore
	// 수집시 현재세션값(시간)등을 등록하여 수집에서 빠져있는지를 확인하여 제거하기 위함
	private String sessionId;

	public abstract String getUniqueKey();
	
	public void clear() {
	}
}	
	
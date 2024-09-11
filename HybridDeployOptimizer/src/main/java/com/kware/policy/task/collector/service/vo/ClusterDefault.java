package com.kware.policy.task.collector.service.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kware.common.db.vo.DefaultDaoVO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class ClusterDefault extends DefaultDaoVO{
	private String    deleteAt = "N";
	
	@JsonIgnore
	private String sessionId; // 수집시 현재세션값(시간)등을 등록하여 수집에서 빠져있는지를 확인하여 제거하기 위함

	public abstract Object getUniqueKey();
	
	public void clear() {
	}
}	
	
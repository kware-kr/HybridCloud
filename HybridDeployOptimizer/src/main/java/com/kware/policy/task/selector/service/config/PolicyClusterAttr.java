package com.kware.policy.task.selector.service.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * 정책서버의 화면으로부터 설정된 데이터를 메모리에 관리함
 */
@Getter
@Setter
@ToString
public class PolicyClusterAttr{
	public static enum CLUSTER_TYPE {
		Dev,Work,Service
	};
	
	private Integer clUid;
	private CLUSTER_TYPE cl_type;
	private int weighted = 1; //1~10사이로 할까? 가중치가 높을 수록 
}
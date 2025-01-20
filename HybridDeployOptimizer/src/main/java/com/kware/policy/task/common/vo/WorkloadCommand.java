package com.kware.policy.task.common.vo;

import lombok.Getter;

@Getter
public class WorkloadCommand<V> {

	public static int CMD_WLD_ENTER    = 1; //워크로드 등록처리 : 
    public static int CMD_WLD_COMPLETE = 2; //워크로드 완료처리 : mlId =>CollectorWorkloadApiWorker에서 처리
    public static int CMD_CON_ENTER    = 3; //컨테이너 등록처리 Container
    public static int CMD_POD_ENTER    = 4; //파드 등록처리   : PromMetricPod => CollectorUnifiedPromMetricWorker
    public static int CMD_WLD_EXPIRED  = 5; //실제 배포가 이루어지지 않아서 삭제 삭제처리 : mlId => RequestQueue
    public static int CMD_NODE_CHANGE  = 11;//노드 정보변경 ://노드가 변경되었거나, gpu가 변경되었을때 기본 특성정보(노드성능, 보안 등 새롭게 생성)
    
	private final int command;
	private final V value;
	
	public WorkloadCommand(int command,V value) {
		this.command = command;
		this.value = value;
	}
}
package com.kware.policy.task.common;

import org.springframework.scheduling.TaskScheduler;

import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;
import com.kware.policy.task.common.queue.WorkloadContainerQueue;

/**
 * 전역으로 사용할 스레드에 안전한 큐와 맵을 생성함
 * @param <T>
 */
public class QueueManager {

	private static final QueueManager instance = new QueueManager();
	
	APIQueue apiQ = null;
	PromQueue promQ = null;
	RequestQueue requestQ = null;
	WorkloadContainerQueue wcontainerQ = null;

	public static QueueManager getInstance() {
		return instance;        
	}
	
	private QueueManager(){
		apiQ = new APIQueue();
		promQ = new PromQueue();
		requestQ = new RequestQueue();
		wcontainerQ = new WorkloadContainerQueue();
	}

	
	TaskScheduler taskScheduler = null;

    
    public void setScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
        apiQ.setScheduler(taskScheduler);
        promQ.setScheduler(taskScheduler);
        requestQ.setScheduler(taskScheduler);
        //wcontainerQ.setScheduler(taskScheduler); //현재는 관련없어서 주석처리함
    }
	
	public APIQueue getApiQ() {
		return apiQ;
	}
	
	public PromQueue getPromQ() {
		return promQ;
	}
	
	public RequestQueue getRequestQ() {
		return requestQ;
	}
	
	public WorkloadContainerQueue getWorkloadContainerQ() {
		return wcontainerQ;
	}
	//---------------------------------------------------------------------------------------------------	
}
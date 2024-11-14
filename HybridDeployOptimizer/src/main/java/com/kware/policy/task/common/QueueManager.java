package com.kware.policy.task.common;

import org.springframework.scheduling.TaskScheduler;

import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;

/**
 * 전역으로 사용할 스레드에 안전한 큐와 맵을 생성함
 * @param <T>
 */
public class QueueManager {

	private static final QueueManager instance = new QueueManager();
	
	APIQueue apiQ = null;
	PromQueue promQ = null;
	RequestQueue requestQ = null;

	public static QueueManager getInstance() {
		return instance;        
	}
	
	private QueueManager(){
		apiQ = new APIQueue();
		promQ = new PromQueue();
		requestQ = new RequestQueue();
	}

	
	TaskScheduler taskScheduler = null;

    
    public void setScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
        apiQ.setScheduler(taskScheduler);
        promQ.setScheduler(taskScheduler);
        requestQ.setScheduler(taskScheduler);
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
	
	
	
	//---------------------------------------------------------------------------------------------------
	
	
	
	
	
	
	
	
}
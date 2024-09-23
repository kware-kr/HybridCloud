package com.kware.policy.task.common.queue;

import org.springframework.scheduling.TaskScheduler;

/**
 * 전역으로 사용할 스레드에 안전한 큐와 맵을 생성함
 * @param <T>
 */

public class DefaultQueue {
	TaskScheduler scheduler = null;
    
    public void setScheduler(TaskScheduler scheduler) {
        this.scheduler = scheduler;
    }    
}
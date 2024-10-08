package com.kware.policy.task.scalor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kware.policy.task.collector.service.PromQLService;
import com.kware.policy.task.scalor.worker.ProcessWorker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//@EnableScheduling // 스케줄링 활성화
@Component
public class ProcessMain { //extends Thread
	
	@Autowired
	private PromQLService service;
	
    private int process_thread_count = 1;
	
    ExecutorService executor = null;
	
    //boolean isRunninig = false;

    public void start() {
		log.info("ProcessMain start");
		//isRunninig = true;
		
        //{{ ExecutorService 생성
        executor = Executors.newFixedThreadPool(this.process_thread_count);
        for(int i = 0 ; i < this.process_thread_count; i++) {
        	ProcessWorker worker = new ProcessWorker();
        	executor.submit(worker);
        }
        //}} ExecutorService 생성 종료
        
        log.info("ProcessMain start end");
	}

	
	public void shutdown() {
		log.info("ProcessMain shutdown");
		
		executor.shutdownNow(); // 스레드 풀 종료 및 인터럽트 요청
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                // 모든 작업이 종료되지 않았을 경우 추가 처리 가능
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 인터럽트 상태 재설정
        }
        
        log.info("ProcessMain shutdown end");
    }
	


}

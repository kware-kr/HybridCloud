package com.kware.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

//스케줄링을 위한 스레드의 갯수
//스프링이 스케쥴을 사용할때 사용하는 스레드 갯수를 제어한다.
@Configuration
public class SchedulerConfig implements SchedulingConfigurer {

	@Value("${hybrid.scheduler.threads:2}")
	int poolThreadCount;
	
	ThreadPoolTaskScheduler scheduler = null;  
	
	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        
		scheduler = (ThreadPoolTaskScheduler)this.threadpoolTaskScheduler();
		
		taskRegistrar.setTaskScheduler(scheduler);
	}
	
	@Bean
	public TaskScheduler threadpoolTaskScheduler() {
		ThreadPoolTaskScheduler threadPoolScheduelr = new ThreadPoolTaskScheduler();  
		 
        // Thread 개수 설정
		//int n = Runtime.getRuntime().availableProcessors();
		//일단 2개를 하드코드로 설정하고 테스트하고, 추 후에 2개만으로 정상동작하지 않으면 yml파일에서 입력하여
		//처리하는 것으로 수정하면 된다.
		//n = 2;
		
		threadPoolScheduelr.setPoolSize(poolThreadCount);
		threadPoolScheduelr.initialize();
		
		return threadPoolScheduelr;
	}
	
}
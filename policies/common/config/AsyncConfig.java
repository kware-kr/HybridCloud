package kware.common.config;

import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;


@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    @Bean(name = "mythreadPoolTaskExecutor")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(3);
        taskExecutor.setMaxPoolSize(30);
        taskExecutor.setQueueCapacity(10);
        taskExecutor.setThreadNamePrefix("Executor?-");
        taskExecutor.initialize();
        
        return taskExecutor;
    }
    
    
    // @PostConstruct 
    // - 의존성 주입이 이루어진 후, 초기화를 수행하는 메서드 (의존성 주입이 끝나고 실행됨이 보장)
    // - bean이 여러번 초기화되는 것을 방지

    // bean 설정에서 init-method 설정을 통해 초기화하는것과 같다.
    // @PostConstruct
    // public void initialize() throws Exception {
    //     log.info("============thread init=================");
    // }
    // @PreDestroy
    // public void destroy() throws Exception{
        
    //     log.info("============thread close=================");
    // }
    
}
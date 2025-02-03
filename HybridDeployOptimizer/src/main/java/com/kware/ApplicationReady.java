package com.kware;

import java.util.TimeZone;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.kware.policy.task.scalor.ScalorMain;

/**
 * @author kljang 어플리케이션이 사용 준비가 되면 최초에 실행해야할 자료나 데이터를 처리할 수 있다.
 */
@Component
public class ApplicationReady {
	static Logger log = LoggerFactory.getLogger(ApplicationReady.class);
	
	//@Autowired
    //private ApplicationContext applicationContext;
	
	ScalorMain pm = null;
	
	public static boolean isApplicationReady = false;
	
	public ApplicationReady() {
	}

	@EventListener(ApplicationReadyEvent.class)
	public void ready() {
		try {
			if(log.isInfoEnabled())
				log.info("ApplicationReady start!!");
			
			if(!init())
				return;
			
			isApplicationReady = true;
			
		}catch(Exception e) {
			return;
		}
		if(log.isInfoEnabled())
			log.info("ApplicationReady end!!");
	}
	
	public boolean init(){
		/* bean 
		String[] beans = applicationContext.getBeanDefinitionNames();

        for (String bean : beans) {
            System.out.println("bean : " + bean);
        }
        */
		
		//여기서 DB를 초기화 할 수 있지만, 최기화 보다 더 빨리 수행하도록 하기 위해서 collect 스케줄러에서 진행하도록 한다.

		pm = new ScalorMain();
		pm.start();
		        
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		
		return true;
	}
	
	@PreDestroy
	public void onExit() throws InterruptedException {
		pm.shutdown();
		log.info("Aplication Shutdown  ===========================================!");
	}

}
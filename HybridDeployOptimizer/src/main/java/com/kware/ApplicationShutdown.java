package com.kware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 *  @author kljang
 *  kill -9가 아닌 정상종료시에 kill -15 정도 되려나
 */
@Component
public class ApplicationShutdown implements ApplicationListener<ContextClosedEvent> {
	protected static final Logger logger = LoggerFactory.getLogger(ApplicationShutdown.class);
	
     @Override
     public void onApplicationEvent(ContextClosedEvent event) {
    	 logger.debug("WebApplicationShutdown ContextClosedEvent!!");
     }
}
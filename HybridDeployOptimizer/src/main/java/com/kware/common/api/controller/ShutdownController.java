package com.kware.common.api.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * kill 보다는 해당api를 호출해서 종료할 수 있도록하고. 나머지 옵션은 process id를 활용하도록 한다.
 * @author kljang
 * 
 * curl -X POST http://localhost:8888/shutdownContext?pid=XXXXX
 *
 */
@RestController
public class ShutdownController implements ApplicationContextAware {
	static Logger log = LoggerFactory.getLogger(ShutdownController.class);
    private ApplicationContext context;
    
    //@PostMapping("/shutdownContext/${req_pid}")  :: @PathVariable("req_pid") String req_pid
    @PostMapping("/shutdownContext")
    public String shutdownContext(HttpServletRequest request) {
    	//curl -X POST localhost:port/shutdownContext
    	String cur_pid = new ApplicationPid().toString();
    	String req_pid = request.getParameter("pid");
    	log.info("ShutdownContext current pid:{}, request pid:{}", cur_pid, req_pid);
    	
    	if(cur_pid.equals(req_pid)) {
    		((ConfigurableApplicationContext) context).close();
    		return "Shutdown OK!!";
    	}
        
    	return "Application pid not Find:" + req_pid ;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.context = ctx;
        
    }
}
package com.kware.policy.task.scalor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kware.policy.task.common.service.CommonService;
import com.kware.policy.task.feature.FeatureMain;
import com.kware.policy.task.scalor.worker.NodeWorker;
import com.kware.policy.task.scalor.worker.PodWorker;

//@Slf4j
@EnableScheduling // 스케줄링 활성화
@Component
public class ScalorMain { //extends Thread
	
/*	
	@Autowired
	private PromQLService pqService;
*/	
	@Autowired
	private CommonService comService;

	
	@Autowired
	private FeatureMain featureMain;
	
	@PostConstruct
	public void init() {		
	}
    
	boolean isFirst = true;
	
	@Scheduled(cron = "30 * * * * ?")
    public void start() {
		if(isFirst) { //1분 후에 시작하기 위함
			isFirst = false;
			return;
		}
		
		NodeWorker nw = new NodeWorker();
//		nw.setPromQLService(pqService);
		nw.setCommonService(comService);
		nw.setFeatureMain(featureMain);
		nw.start();
		
		
		PodWorker pw = new PodWorker();
//		pw.setPromQLService(pqService);
		pw.setCommonService(comService);
		pw.setFeatureMain(featureMain);
		pw.start();
	}

	
    @PreDestroy
	public void shutdown() {
    }
}

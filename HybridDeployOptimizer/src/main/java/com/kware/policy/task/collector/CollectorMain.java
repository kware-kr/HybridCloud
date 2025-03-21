package com.kware.policy.task.collector;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kware.policy.task.collector.service.ClusterManagerService;
import com.kware.policy.task.collector.service.PromQLService;
import com.kware.policy.task.collector.service.ResourceUsageService;
import com.kware.policy.task.collector.service.vo.PromQL;
import com.kware.policy.task.collector.worker.CollectorClusterApiWorker;
import com.kware.policy.task.collector.worker.CollectorUnifiedPromMetricWorker;
import com.kware.policy.task.collector.worker.CollectorWorkloadApiWorker;
import com.kware.policy.task.collector.worker.ResourceUsageDBSaveWorker;
import com.kware.policy.task.common.PromQLManager;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.WorkloadCommandManager;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.PromQueue.PromDequeName;
import com.kware.policy.task.common.service.CommonService;
import com.kware.policy.task.feature.FeatureMain;
import com.kware.policy.task.scalor.service.ScalingInfoService;
import com.kware.policy.task.selector.service.WorkloadRequestService;

import lombok.extern.slf4j.Slf4j;



@Slf4j
@EnableScheduling // 스케줄링 활성화
@Component
@DependsOn("initDatabase")  //중요함
public class CollectorMain {
	
	private final TaskScheduler taskScheduler;

 //   @Autowired
    public CollectorMain(@Qualifier("threadpoolTaskScheduler") TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }
    
	@Autowired
	private PromQLService ptService;
	
	@Autowired
	private CommonService comService;
	
	@Autowired
	private ScalingInfoService scalingService;
	
	@Autowired
	private ClusterManagerService cmService;
	
	@Autowired
	private ResourceUsageService ruService;
	
	@Autowired
	private WorkloadRequestService wrService;
	
	@Autowired
	private FeatureMain feaMain;
	
	@Value("${hybrid.collector.threads}")
    private int col_threads_nu;
	
	@Value("${hybrid.collector.portal-api.prometheus.authorization:null}")
	private String prometheus_authorization_token;
	
	@Value("${hybrid.collector.portal-api.url}")
	private String api_base_url;
	
	@Value("${hybrid.collector.portal-api.authorization}")
	private String api_authorization_token;
	
	@Value("${hybrid.collector.portal-api.delete-enalbe:false}")
	private boolean api_delete_enable ;
	@Value("${hybrid.collector.portal-api.finish-enalbe:false}")
	private boolean api_finish_enable ;
	
	@Value("${hybrid.collector.portal-api.prometheus.unified_url:null}")
	private String api_prometheus_unified_url;
	
	final QueueManager qm = QueueManager.getInstance();
	final WorkloadCommandManager wcm = WorkloadCommandManager.getInstance();
	
	APIQueue  apiQ  = qm.getApiQ();
	PromQueue promQ = qm.getPromQ();
	//WorkloadContainerQueue wcQ = qm.getWorkloadContainerQ();
	
	boolean isFirst = false;
	
	@PostConstruct  // 애플리케이션 시작 시 한 번 실행할 로직
	@SuppressWarnings("unchecked")
    public void runOnceOnStartup() {
		
		qm.setScheduler(taskScheduler);
		
		try {//초기에 5초간의 여유를 주가 실행한다.
			isFirst = true;
			collectClusterTask();
			
			Thread.sleep(5000);
			collectWorkloadTask();
			
			Thread.sleep(5000);
			collectMetricTaskUnified();
			
			isFirst = false;
			
			
			//requestService 등록
			//wcm.setWorkloadRequestService(wrService);
			//wcm.setCommonService(comService);
			wcm.setInitSevice(wrService, comService, feaMain, scalingService);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
    }
	
	
//	@Scheduled(cron = "0 0/1 * * * *") // 매 X분마다 실행하며 이작업은 이전 호출이 완료된 시점부터 계산된다.
	//@Scheduled(initialDelay = 5000, fixedDelay = 60000) 
	
	public final class Cron {
	    private Cron() {}
	    private static final String minute = "0/1";    //테스트 중에 변경하기 쉽도록  x/y   x분시작해서, y 간격
	
	    public static final String collectClusterTask_cron       =  "0 "  + minute + " * * * *"; 
	    public static final String collectWorkloadTask_cron      =  "20 " + minute + " * * * *";
	    public static final String collectMetricTaskUnified_cron =  "40 " + minute + " * * * *";
	    public static final String ResourceUsageTask_cron        =  "50 " + minute + " * * * *";
	    
	}
	
	/**
	 * 클러스터, 클러스터 노드 수집
	 */
	//@Scheduled(cron = "0 * * * * *") // 1분 스케줄링
	@Scheduled(cron = CollectorMain.Cron.collectClusterTask_cron) // 1분 스케줄링
	public void collectClusterTask() {
		
		if(log.isDebugEnabled()) {
			log.debug("CollectClusterTask 시작");
		}
		
		CollectorClusterApiWorker worker = new CollectorClusterApiWorker();
		worker.setClusterManagerService(cmService);
		worker.setApiBaseUrl(api_base_url);
		worker.setAuthorizationToken(api_authorization_token);
		worker.setIsFirst(isFirst);
		worker.start();
		
	}
	
//	@Scheduled(cron = "0 0/1 * * * *") // 매 X분마다 실행하며 이작업은 이전 호출이 완료된 시점부터 계산된다.
	//@Scheduled(initialDelay = 5000, fixedDelay = 60000)
	
	/**
	 * 각 클러스터에서 운영되는 워크로드 수집
	 */
	@Scheduled(cron = CollectorMain.Cron.collectWorkloadTask_cron) 
	public void collectWorkloadTask() {
		
		if(log.isDebugEnabled()) {
			log.debug("collectWorkloadTask 시작");
		}
		
		CollectorWorkloadApiWorker worker = new CollectorWorkloadApiWorker();
		worker.setClusterManagerService(cmService);
		worker.setCommonService(comService);
		worker.setApiBaseUrl(api_base_url);
		worker.setAuthorizationToken(api_authorization_token);
		worker.setPropertyForAPI(this.api_finish_enable, this.api_delete_enable);
		worker.setName(worker.getClass().getName());
		worker.setIsFirst(isFirst);
		worker.start();
	}
	
	@Scheduled(cron = CollectorMain.Cron.collectMetricTaskUnified_cron) //
	public void collectMetricTaskUnified() {
		
		if(log.isDebugEnabled()) {
			log.debug("collectMetricTaskUnified 시작");
		}
		if(this.api_prometheus_unified_url == null) {
			log.debug("collectMetricTaskTotal 종료: Prometheus Unified URL isnull" );
			return;
		}
				
		//DB테이블에서 쿼리의종류인 promql 리스트를 조회한다.
		//metric extract 정보를 전체 조회한다.
		List<PromQL> lPromqlList = this.ptService.selectPromqlListAll();
		PromQLManager mp = PromQLManager.getInstance();
		for(PromQL p: lPromqlList) {
			mp.setExtractPath(p);
		}
		lPromqlList.clear();
		
		//{{ 큐에 먼저 등록한다.: 큐는 순서가 중요해서 각 스레드에서 등록하면, 동일한 시간 데이터가 여러개 들어가서,
		// 메인 스케줄러에서 등록해서 해당 데이터를 제공한다.
		//lifo구조를 사용한다.
		Long current_millitime = System.currentTimeMillis();
		/*
		PromMetricNodes nodes = new PromMetricNodes();
		nodes.setTimestamp(current_millitime);
		log.info("nodelist 등록");
		nodeDeque.addFirst(nodes);
		
		
		PromMetricPods  pods = new PromMetricPods();
		pods.setTimestamp(current_millitime);
		log.info("podlist 등록");
		podDeque.addFirst(pods);
		*/
		//}}

		//{{ 통합된 프로메테우스 활용
		CollectorUnifiedPromMetricWorker worker = new CollectorUnifiedPromMetricWorker(current_millitime);
		worker.setPrometheusService(this.ptService);
		worker.setClusterManagerService(this.cmService);

		worker.setPrometheusUrl(this.api_prometheus_unified_url);
		//worker.setThreadsNumber(this.col_threads_nu);
		worker.setThreadsNumber(3);
		//worker.setAuthorizationToken(prometheus_authorization_token);
		worker.start();
		//}}
			
		if(log.isDebugEnabled()) {
			log.debug("current {} nodeDeque size={}", PromDequeName.METRIC_NODEINFO.toString(), promQ.getPromDequesSize(PromDequeName.METRIC_NODEINFO));
			log.debug("current {} podDeque size={}" , PromDequeName.METRIC_PODINFO.toString() , promQ.getPromDequesSize(PromDequeName.METRIC_PODINFO));
		}
	}
	
	@Scheduled(cron = CollectorMain.Cron.ResourceUsageTask_cron)// 
	public void ResourceUsageTask() {
		
		if(log.isDebugEnabled()) {
			log.debug("ResourceUsageTask 시작");
		}
		
		//{{ 노드와 파드의 리소스 사용량을 매 분마다 저장: 추후 모니터링 그래프 활용할 수 있도록
		ResourceUsageDBSaveWorker worker = new ResourceUsageDBSaveWorker();
		worker.setResourceUsageServiceService(this.ruService);
		worker.start();
		//}}
	}
}

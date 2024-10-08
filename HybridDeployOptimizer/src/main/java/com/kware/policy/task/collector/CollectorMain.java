package com.kware.policy.task.collector;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import com.kware.common.db.InitDatabase;
import com.kware.policy.task.collector.service.ClusterManagerService;
import com.kware.policy.task.collector.service.PromQLService;
import com.kware.policy.task.collector.service.ResourceUsageService;
import com.kware.policy.task.collector.service.vo.Cluster;
import com.kware.policy.task.collector.service.vo.PromQL;
import com.kware.policy.task.collector.worker.CollectorClusterWorker;
import com.kware.policy.task.collector.worker.CollectorSinglePromMetricWorker;
import com.kware.policy.task.collector.worker.CollectorUnifiedPromMetricWorker;
import com.kware.policy.task.collector.worker.CollectorWorkloadWorker;
import com.kware.policy.task.collector.worker.ResourceUsageWorker;
import com.kware.policy.task.common.PromQLManager;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.PromQueue.PromDequeName;

import lombok.extern.slf4j.Slf4j;



@Slf4j
@EnableScheduling // 스케줄링 활성화
@Component
public class CollectorMain {
	
	private final TaskScheduler taskScheduler;

 //   @Autowired
    public CollectorMain(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }
    
	@Autowired
	private PromQLService ptService;
	
	@Autowired
	private ClusterManagerService cmService;
	
	@Autowired
	private ResourceUsageService ruService;
	
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
	
	APIQueue  apiQ  = qm.getApiQ();
	PromQueue promQ = qm.getPromQ();
	
	boolean isFirst = false;
	
	@Autowired
	InitDatabase initDatabase;
	
	@PostConstruct  // 애플리케이션 시작 시 한 번 실행할 로직
	@SuppressWarnings("unchecked")
    public void runOnceOnStartup() {
		/*
        //cluster, clusterNode를 DB에서 가져와서 등록한다.	
		Map<String, Cluster> clusterApiMap = apiQ.getApiClusterMap();
		List<Cluster> clusterList = cmService.selectClusterList(null);
		for(Cluster cl: clusterList) {
			clusterApiMap.put(Integer.toString(cl.getUid()), cl);
		}
		
		Map<String, ClusterNode> nodeApiMap = apiQ.getApiClusterNodeMap();
		List<ClusterNode> nodeList = cmService.selectClusterNodeList(null);
		for(ClusterNode n: nodeList) {
			nodeApiMap.put(n.getUniqueKey(), n);
		}
		*/
		
		//{{Database 초기 테이블 생성하고, 초기 데이터를 등록 
		// applicationready event는 WAS가 준비되었다는 거고, Web과 일반 application이 혼재하게 구성되어 있으며, web은 sub구성임. 
		try {
			initDatabase.initializeDatabase();
		} catch (Exception e) {
			ThreadPoolTaskScheduler a = (ThreadPoolTaskScheduler)this.taskScheduler;
			a.shutdown();
			log.error("Database 초기화 오류", e);
			return;
		}
		//}}
		
		
		
		qm.setScheduler(taskScheduler);
		
		try {//초기에 5초간의 여유를 주가 실행한다.
			isFirst = true;
			collectClusterTask();
			
			Thread.sleep(5000);
			collectWorkloadTask();
			
			Thread.sleep(5000);
			collectMetricTaskUnified();
			
			isFirst = false;
		}catch(Exception e) {
			e.printStackTrace();
		}
		
    }
	
	
//	@Scheduled(cron = "0 0/1 * * * *") // 매 X분마다 실행하며 이작업은 이전 호출이 완료된 시점부터 계산된다.
	//@Scheduled(initialDelay = 5000, fixedDelay = 60000) 
	
	/**
	 * 클러스터, 클러스터 노드 수집
	 */
	@Scheduled(cron = "0 * * * * *") // 1분 스케줄링
	public void collectClusterTask() {
		
		if(log.isDebugEnabled()) {
			log.debug("CollectClusterTask 시작");
		}
		
		CollectorClusterWorker worker = new CollectorClusterWorker();
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
	@Scheduled(cron = "20 * * * * *") // 1분 스케줄링
	public void collectWorkloadTask() {
		
		if(log.isDebugEnabled()) {
			log.debug("collectWorkloadTask 시작");
		}
		
		CollectorWorkloadWorker worker = new CollectorWorkloadWorker();
		worker.setClusterManagerService(cmService);
		worker.setApiBaseUrl(api_base_url);
		worker.setAuthorizationToken(api_authorization_token);
		worker.setPropertyForAPI(this.api_finish_enable, this.api_delete_enable);
		worker.setName(worker.getClass().getName());
		worker.setIsFirst(isFirst);
		worker.start();
	}
		
	//클러스터별 프로메테우스 운영: 통합으로 변경되면서 사용안함.
	//@Scheduled(initialDelay = 5000, fixedDelay = 60000) 
	public void collectMetricTaskSingle() {
		
		if(log.isDebugEnabled()) {
			log.debug("collectMetricTask 시작");
		}
				
		//DB테이블에서 쿼리의종류인 promql 리스트를 조회한다.
		//metric extract 정보를 전체 조회한다.
		List<PromQL> lPromqlList = ptService.selectPromqlListAll();
		PromQLManager mp = PromQLManager.getInstance();
		for(PromQL p: lPromqlList) {
			mp.setExtractPath(p);
		}
		
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

		//{{ 각 클러스터별로 프로메테우스를 가져올때는 클러스터에 있는 플로메테우스의 URL를 가지고 처리한다.
		List<Cluster> lClusterList = ptService.selectClusterList();
		Cluster rs = null;
		for(int i = 0 ; i < lClusterList.size(); i++) {
		//for(int i = 0 ; i < 1; i++) {
			rs = lClusterList.get(i);
			CollectorSinglePromMetricWorker worker = new CollectorSinglePromMetricWorker(current_millitime);
			worker.setPrometheusService(ptService);
			worker.setClusterInfo(rs);
			//worker.setThreadsNumber(this.col_threads_nu);
			worker.setThreadsNumber(1);
			worker.setAuthorizationToken(prometheus_authorization_token);
			worker.start();
		}
		//}}
		
		if(log.isDebugEnabled()) {
			log.debug("current {} nodeDeque size={}", PromDequeName.METRIC_NODEINFO.toString(), promQ.getPromDequesSize(PromDequeName.METRIC_NODEINFO));
			log.debug("current {} podDeque size={}" , PromDequeName.METRIC_PODINFO.toString() , promQ.getPromDequesSize(PromDequeName.METRIC_PODINFO));
		}
		
		if(lClusterList != null) {
			lClusterList.clear();
			lClusterList = null;
		}
	}
	
	//@Scheduled(cron = "0 0 8-23 * * *") // 매 1분마다 실행하며 이작업은 이전 호출이 완료된 시점부터 계산된다.
	//@Scheduled(initialDelay = 5000, fixedDelay = 60000) 
	
	
	
	@Scheduled(cron = "40 * * * * *") // 30초마다 스케줄링
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
	
	@Scheduled(cron = "50 * * * * *") // 30초마다 스케줄링
	public void ResourceUsageTask() {
		
		if(log.isDebugEnabled()) {
			log.debug("ResourceUsageTask 시작");
		}
		
		//{{ 노드와 파드의 리소스 사용량을 매 분마다 저장: 추후 모니터링 그래프 활용할 수 있도록
		ResourceUsageWorker worker = new ResourceUsageWorker();
		worker.setResourceUsageServiceService(this.ruService);
		worker.start();
		//}}
	}
}

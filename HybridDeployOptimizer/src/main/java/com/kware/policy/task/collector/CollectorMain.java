package com.kware.policy.task.collector;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kware.policy.common.PromQLManager;
import com.kware.policy.common.QueueManager;
import com.kware.policy.common.QueueManager.PromDequeName;
import com.kware.policy.service.ClusterManagerService;
import com.kware.policy.service.PromQLService;
import com.kware.policy.service.vo.Cluster;
import com.kware.policy.service.vo.ClusterNode;
import com.kware.policy.service.vo.PromQL;
import com.kware.policy.task.collector.worker.CollectorClusterWorker;
import com.kware.policy.task.collector.worker.CollectorSinglePromMetricWorker;
import com.kware.policy.task.collector.worker.CollectorUnifiedPromMetricWorker;
import com.kware.policy.task.collector.worker.CollectorWorkloadWorker;

import lombok.extern.slf4j.Slf4j;



@Slf4j
@EnableScheduling // 스케줄링 활성화
@Component
public class CollectorMain {
    
	@Autowired
	private PromQLService ptService;
	
	@Autowired
	private ClusterManagerService cmService;
	
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
	
	//{{프로메테우스를 통해서 조회된 노드, 파드 정보
	//@SuppressWarnings("unchecked")
	//BlockingDeque<PromMetricNodes> nodeDeque = (BlockingDeque<PromMetricNodes>)qm.getPromDeque(QueueManager.PromDequeName.METRIC_NODEINFO);
	//@SuppressWarnings("unchecked")
	//BlockingDeque<PromMetricPods>  podDeque  = (BlockingDeque<PromMetricPods>)qm.getPromDeque(QueueManager.PromDequeName.METRIC_PODINFO);
	//}
	
	@PostConstruct  // 애플리케이션 시작 시 한 번 실행할 로직
	@SuppressWarnings("unchecked")
    public void runOnceOnStartup() {
        //cluster, clusterNode를 DB에서 가져와서 등록한다.	
		ConcurrentHashMap<String, Cluster> clusterApiMap = (ConcurrentHashMap<String, Cluster>)qm.getApiMap(QueueManager.APIMapsName.CLUSTER);
		List<Cluster> clusterList = cmService.selectClusterList(null);
		for(Cluster cl: clusterList) {
			clusterApiMap.put(Integer.toString(cl.getUid()), cl);
		}
		
		ConcurrentHashMap<String, ClusterNode> nodeApiMap = (ConcurrentHashMap<String, ClusterNode>)qm.getApiMap(QueueManager.APIMapsName.NODE);
		List<ClusterNode> nodeList = cmService.selectClusterNodeList(null);
		for(ClusterNode n: nodeList) {
			nodeApiMap.put(n.getUniqueKey(), n);
		}
    }
	
	
//	@Scheduled(cron = "0 0/1 * * * *") // 매 2분마다 실행하며 이작업은 이전 호출이 완료된 시점부터 계산된다.
	@Scheduled(initialDelay = 5000, fixedDelay = 60000) 
	//@Scheduled(cron = "0,15,30,45 * * * * *") // 15초마다 스케줄링
	public void collectClusterTask() {
		if(log.isDebugEnabled()) {
			log.debug("CollectClusterTask 시작");
		}
		
		CollectorClusterWorker worker = new CollectorClusterWorker();
		worker.setClusterManagerService(cmService);
		worker.setApiBaseUrl(api_base_url);
		worker.setAuthorizationToken(api_authorization_token);
		worker.start();
		
	}
	
//	@Scheduled(cron = "0 0/1 * * * *") // 매 2분마다 실행하며 이작업은 이전 호출이 완료된 시점부터 계산된다.
	@Scheduled(initialDelay = 5000, fixedDelay = 60000)
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
		worker.start();
	}
		
	//클러스터별 프로메테우스 운영: 통합으로 변경되면서 사용안함.
	//@Scheduled(initialDelay = 5000, fixedDelay = 60000) 
	public void collectMetricTask() {
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
			log.debug("current {} nodeDeque size={}", QueueManager.PromDequeName.METRIC_NODEINFO.toString(), qm.getPromDequesSize(PromDequeName.METRIC_NODEINFO));
			log.debug("current {} podDeque size={}" , QueueManager.PromDequeName.METRIC_PODINFO.toString() , qm.getPromDequesSize(PromDequeName.METRIC_PODINFO));
		}
		
		if(lClusterList != null) {
			lClusterList.clear();
			lClusterList = null;
		}
	}
	
	//@Scheduled(cron = "0 0 8-23 * * *") // 매 1분마다 실행하며 이작업은 이전 호출이 완료된 시점부터 계산된다.
	@Scheduled(initialDelay = 5000, fixedDelay = 60000) 
	//@Scheduled(cron = "0,15,30,45 * * * * *") // 15초마다 스케줄링
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

		worker.setPrometheusUrl(this.api_prometheus_unified_url);
		//worker.setThreadsNumber(this.col_threads_nu);
		worker.setThreadsNumber(1);
		//worker.setAuthorizationToken(prometheus_authorization_token);
		worker.start();
		//}}
			
		if(log.isDebugEnabled()) {
			log.debug("current {} nodeDeque size={}", QueueManager.PromDequeName.METRIC_NODEINFO.toString(), qm.getPromDequesSize(PromDequeName.METRIC_NODEINFO));
			log.debug("current {} podDeque size={}" , QueueManager.PromDequeName.METRIC_PODINFO.toString() , qm.getPromDequesSize(PromDequeName.METRIC_PODINFO));
		}
		
	}
}
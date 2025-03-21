/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kware.policy.task.collector.worker;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kware.common.util.HttpSSLFactory;
import com.kware.policy.task.collector.service.ClusterManagerService;
import com.kware.policy.task.collector.service.PromQLService;
import com.kware.policy.task.collector.service.vo.ClusterNode;
import com.kware.policy.task.collector.service.vo.PromMetricNodes;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.collector.service.vo.PromMetricPods;
import com.kware.policy.task.collector.service.vo.PromQL;
import com.kware.policy.task.collector.service.vo.PromQLResult;
import com.kware.policy.task.collector.worker.anal.MetricResultAnalyzer;
import com.kware.policy.task.common.PromQLManager;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.WorkloadCommandManager;
import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.PromQueue.PromDequeName;
import com.kware.policy.task.common.queue.RequestQueue;
import com.kware.policy.task.common.queue.WorkloadContainerQueue;
import com.kware.policy.task.common.service.CommonService;
import com.kware.policy.task.common.vo.WorkloadCommand;

import lombok.extern.slf4j.Slf4j;

/**
 * ==>한개의 프로메테우스로 통합된 프로메테우스에 접근해서 수집( 클러스터 구분은 쿼리결과의 ref_id 속성이 API에서 제공한 클러스터 아이
 * 프로메테우스에 접근해서 결과를 Queue에 등록하고, DB에도 그대로 저장한다.
 * Queue에 등록시에는 쿼리결과를 그대로 저장하고(여러개의 키가 포함된 json) ==> Queue를 처리하는 processMain에서 각각 알고리즘에 맞게 파싱해서 처리한다.
 * DB에 저장할때는 쿼리켤과를 파싱해서 각 키별로 저장한다. data:{ result:[{},{}]}인데 각 배열값에는 metric: value 가 있는 각1개씩 DB에 저장한다.
 */
@Slf4j
@SuppressWarnings({"unchecked"})
public class CollectorUnifiedPromMetricWorker extends Thread {

	private static final Logger metricLog = LoggerFactory.getLogger("metric-log");
	private static String prometheus_uri = "api/v1/query";
	
	final PromQLManager mp = PromQLManager.getInstance();
	final QueueManager qm = QueueManager.getInstance();
	
	APIQueue  apiQ  = qm.getApiQ();
	PromQueue promQ = qm.getPromQ();
	RequestQueue  requestQ = qm.getRequestQ();
	WorkloadContainerQueue wlContainerQ = qm.getWorkloadContainerQ();
	
	private final String STR_query  = "query";
	
	PromQLService service = null;
	int threadsNubmer = 2;
	boolean isRunning = false;
	
	//Cluster clusterInfo = null;
	String prometheus_url = null;
	
	private String authorization_token = null;
	
	final Long current_millitime;
	final Timestamp timestamp;
	final PromMetricNodes prom_nodes = new PromMetricNodes();
	final PromMetricPods  prom_pods = new PromMetricPods();
	

	public CollectorUnifiedPromMetricWorker(Long _current_millitime) {
		this.current_millitime = _current_millitime;
		
		this.prom_nodes.setTimemillisecond(this.current_millitime);
		this.prom_pods.setTimemillisecond(this.current_millitime);
		this.timestamp = new Timestamp(current_millitime);
		
	}

	public boolean isRunning() {
		return this.isRunning;
	}
	
	public void setThreadsNumber(int col_threads_nu) {
		this.threadsNubmer = col_threads_nu;
	}
/*
	public void setClusterInfo(Cluster rs) {
		this.clusterInfo = rs;
	}
*/	
	public void setPrometheusUrl(String _url) {
		this.prometheus_url = _url;
	}

	public void setPrometheusService(PromQLService pService) {
		this.service = pService;
	}
	
	ClusterManagerService cmService = null;
	public void setClusterManagerService(ClusterManagerService cmService) {
		this.cmService = cmService;
	}
	
	//공통 서비스
	CommonService comService = null;
	public void setCommonService(CommonService comService) {
		this.comService = comService;
	}
	
	public void setAuthorizationToken(String authorization_token) {
		this.authorization_token = authorization_token;
	}

	@Override
	public void run() {
		isRunning = true;
		try {
			String prometheus_url;
			if(this.prometheus_url.endsWith("/")) {
				prometheus_url = this.prometheus_url + prometheus_uri;
			}else {
				prometheus_url = this.prometheus_url + "/" + prometheus_uri;
			}
	
			ExecutorService executorService = Executors.newFixedThreadPool(this.threadsNubmer);		
	
			PromQL promQl = new PromQL();
//			promQl.setClUid(clusterInfo.getUid());
			List<Integer> promqlIdList = service.selectPromqlIdList(promQl);
			
			for (Integer promqlId : promqlIdList) {
				PromQL promqlInfo = mp.getPromQL(promqlId);
				String query = (String) promqlInfo.getCont();
	
				final Runnable runnable = () -> {
					String result = null;
					Map<String,String> params = new HashMap<String, String>();			
					try {
						params.put(this.STR_query, query);
						result = this.getPrometheusResult(prometheus_url, org.jsoup.Connection.Method.GET, params, null, promqlId);
						
						//DB입력
						this.insertResult(null, promqlInfo, result);
						
						//결과분석
						MetricResultAnalyzer analyzer = new MetricResultAnalyzer(this.prom_nodes, this.prom_pods, this.current_millitime);
						analyzer.analyze(null, promqlInfo.getPrqlUid(), result);
					} catch (IOException e) {
						log.error(e.toString(), e);
					}
					params.clear();
					params = null;
				};						
				executorService.execute(runnable);
			}
			
			executorService.shutdown();
			
			try {
				//{{ 테스트에만 사용:문제 발생시 계속 대기할 수 있어서 운영에서는 사용하기가 부담스럽다. count 변수 적용이 필요함.
				//while(!executorService.isTerminated()) {sleep(10);}
				//}}
				//shutdown 후에 모든 job이 종료될 동안 대기한다. 최대 10초를 대기한다.
				executorService.awaitTermination(10, TimeUnit.SECONDS);
				
				//{{노드관련 API로 수집한 정보를 현재 수집한 데이터에 추가 처리.
				//key: node.getUniqueKey() val:ClusterNode
				log.info("=========================================================================================================");
				this.prom_nodes.getAllNodeList().forEach(k->{
					ClusterNode cnode = this.apiQ.getApiClusterNodeMap().get(k.getClUid() + "_" + k.getNode());
					if(cnode == null)
						return;
					
					k.setUid(cnode.getUid());//DB key값
					k.setNoUuid(cnode.getNoUuid());  //node uuid 설정
					k.setStatus(cnode.getStatus());  // 상태 설정
					/*
					try {
						log.info("{}",JSONUtil.getJsonstringFromObject(k));
					} catch (JsonProcessingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					*/
					
					k.addLabels(cnode.getLabels());  // labels 추가
				});
				log.info("=========================================================================================================");
				//}}API에만 있는 데이터 추가 처리.
				
				//{{
				//requestQ, WorkloadContainerQueue에 데이터 추가작업 진행
				
				this.prom_pods.getAllPodList().forEach(k->{
					//ContainerWrapper class update
					WorkloadCommand<PromMetricPod> command = new WorkloadCommand<PromMetricPod>(WorkloadCommand.CMD_POD_ENTER,k);
					WorkloadCommandManager.addCommand(command);
				});
				
				//}}
			} catch (InterruptedException e) {
				log.error("awaitTermination Error:", e);
			}

			//여기서 블러킹 큐에 등록해야겠다.
			this.promQ.addPromDequesObject(PromDequeName.METRIC_NODEINFO, this.prom_nodes);
			this.promQ.addPromDequesObject(PromDequeName.METRIC_PODINFO , this.prom_pods);
			
			
			if(log.isDebugEnabled()) {
				log.info("***********************************************************************************************************");
				log.debug("프로메테우스 전체 노드:{}", this.prom_nodes.getAllNodeList());
				log.info("-----------------------------------------------------------------------------------------------------------");
				log.debug("프로메테우스 전체 파드:{}", this.prom_pods.getAllPodList());
				log.info("***********************************************************************************************************");
			}
			
			//미리 등록하면 빈 껌질을 가지고 처리하는 경우가 발생하네
			
			promqlIdList.clear();
			promqlIdList = null;
		}catch(Exception e) {
			log.error("MetricWorker Thread Error:",e);
		}
	}

	private void insertResult(Integer cl_uid, PromQL promqlInfo, String rsJsonString) {
		int prql_uid = promqlInfo.getPrqlUid();
		
		//{{DB 입력
		PromQLResult plResult = new PromQLResult();
		try {
			plResult.setClUid(cl_uid);
			plResult.setPrqlUid(prql_uid);
			plResult.setResults(rsJsonString);
			plResult.setCollectDt(this.timestamp);
			
			service.insertPromqlResult(plResult);
		} catch (IllegalStateException e) {
			log.error("queue put error", e);
		}
		//}}DB 입력
	}

	private static Instant lastLogTime = Instant.now();

	private String getPrometheusResult(String url, org.jsoup.Connection.Method method, Map<String, String> params, String bodyString, Integer promqlId) throws IOException {
		
		org.jsoup.Connection connection = Jsoup.connect(url)
				.method(method)
				.timeout(30 * 1000)
				.followRedirects(true)
				// .validateTLSCertificates(false)
				.sslSocketFactory(HttpSSLFactory.socketFactory())
				.ignoreContentType(true);
		String logQuery = null;
		
		if(params != null) {
			connection.data(params);
			logQuery = params.toString();
		}
		
		if(bodyString != null) {
			connection.header(StringConstant.STR_Content_Type, StringConstant.STR_application_json);
			connection.requestBody(bodyString);
			logQuery = bodyString;
		}
		
		if(this.authorization_token != null) {
			connection.header(StringConstant.STR_Authorization, this.authorization_token);
		}
		
		org.jsoup.Connection.Response dataPage = connection.execute();

		if (log.isDebugEnabled()) {
			log.debug("수집상태코드 prmqlId={}, {}", promqlId, dataPage.statusCode());
		}

		String json_string = dataPage.body();
		
		//metricLog.debug("##promqlId={}\t\turl={}",promqlId, url);
		
		
		Instant now = Instant.now();
        // 10분(600초) 경과 여부 확인
        if (now.minusSeconds(600).isAfter(lastLogTime)) {
        	if (metricLog.isDebugEnabled())
    			metricLog.debug("##Request[id={}]\nquery={}\n##Response:{}",promqlId, url, logQuery, json_string);
            lastLogTime = now;
        }else {
        	if (metricLog.isDebugEnabled())
    			metricLog.debug("##Request[id={}]",promqlId);
        }
        
		return json_string;
	}
}

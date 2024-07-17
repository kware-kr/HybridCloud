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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kware.common.util.HttpSSLFactory;
import com.kware.policy.common.PromQLManager;
import com.kware.policy.common.QueueManager;
import com.kware.policy.service.PromQLService;
import com.kware.policy.service.vo.Cluster;
import com.kware.policy.service.vo.ClusterNode;
import com.kware.policy.service.vo.PromMetricNodes;
import com.kware.policy.service.vo.PromMetricPods;
import com.kware.policy.service.vo.PromQL;
import com.kware.policy.service.vo.PromQLResult;
import com.kware.policy.task.StringConstant;
import com.kware.policy.task.collector.worker.anal.MetricResultAnalyzer;

import lombok.extern.slf4j.Slf4j;

/**
 * ==>클러스터별로 개별적인 프로메테우스에 접근해서 수집 
 * 
 * 프로메테우스에 접근해서 결과를 Queue에 등록하고, DB에도 그대로 저장한다.
 * Queue에 등록시에는 쿼리결과를 그대로 저장하고(여러개의 키가 포함된 json) ==> Queue를 처리하는 processMain에서 각각 알고리즘에 맞게 파싱해서 처리한다.
 * DB에 저장할때는 쿼리켤과를 파싱해서 각 키별로 저장한다. data:{ result:[{},{}]}인데 각 배열값에는 metric: value 가 있는 각1개씩 DB에 저장한다.
 */
@Slf4j
@SuppressWarnings({"unchecked"})
public class CollectorSinglePromMetricWorker extends Thread {

	private static final Logger metricLog = LoggerFactory.getLogger("metric-log");
	private static String prometheus_uri = "/api/v1/query";
	
	final QueueManager qm = QueueManager.getInstance();
	
	//BlockingQueue<Map> queue = queueManager.getQueue(QueueManager.QueueName.METRIC_COLLECT);
	BlockingDeque<PromMetricNodes> nodeDeque = (BlockingDeque<PromMetricNodes>)qm.getPromDeque(QueueManager.PromDequeName.METRIC_NODEINFO);
	BlockingDeque<PromMetricPods>  podDeque  = (BlockingDeque<PromMetricPods>)qm.getPromDeque(QueueManager.PromDequeName.METRIC_PODINFO);
	
	//key:cluid val:Cluster
	ConcurrentHashMap<String, Cluster> clusterApiMap  = (ConcurrentHashMap<String, Cluster>)qm.getApiMap(QueueManager.APIMapsName.CLUSTER);
	//key: node.getUniqueKey() val:ClusterNode
	ConcurrentHashMap<String, ClusterNode> nodeApiMap = (ConcurrentHashMap<String, ClusterNode>)qm.getApiMap(QueueManager.APIMapsName.NODE);
	
	PromQLManager mp = PromQLManager.getInstance();
	
	private final String STR_query  = "query";
	
	PromQLService service = null;
	int threadsNubmer = 2;
	boolean isRunning = false;
	Cluster clusterInfo = null;
	
	private String authorization_token = null;
	
	final Long current_millitime;
	final Timestamp timestamp;
	final PromMetricNodes prom_nodes = new PromMetricNodes();
	final PromMetricPods  prom_pods = new PromMetricPods();
	

	public CollectorSinglePromMetricWorker(Long _current_millitime) {
		this.current_millitime = _current_millitime;
		
		this.prom_nodes.setTimestamp(this.current_millitime);
		this.prom_pods.setTimestamp(this.current_millitime);
		this.timestamp = new Timestamp(current_millitime);
	}

	public boolean isRunning() {
		return this.isRunning;
	}
	
	public void setThreadsNumber(int col_threads_nu) {
		this.threadsNubmer = col_threads_nu;
	}

	public void setClusterInfo(Cluster rs) {
		this.clusterInfo = rs;
	}

	public void setPrometheusService(PromQLService pService) {
		this.service = pService;
	}
	
	public void setAuthorizationToken(String authorization_token) {
		this.authorization_token = authorization_token;
	}

	@Override
	public void run() {
		isRunning = true;
		try {
			String prometheus_url = (String) clusterInfo.getPromUrl() + prometheus_uri;
	
			ExecutorService executorService = Executors.newFixedThreadPool(this.threadsNubmer);		
	
			PromQL promQl = new PromQL();
			promQl.setClUid(clusterInfo.getUid());
			List<Integer> promqlIdList = service.selectPromqlIdList(promQl);
			
			for (Integer promqlId : promqlIdList) {
				PromQL promqlInfo = mp.getPromQL(promqlId);
				String query = (String) promqlInfo.getCont();
	
				final Runnable runnable = () -> {
					String result = null;
					Map<String,String> params = new HashMap<String, String>();			
					try {
						params.put(this.STR_query, query);
						result = this.getPrometheusResult(prometheus_url, org.jsoup.Connection.Method.GET, params, null);
						
						//DB입력
						this.insertResult(clusterInfo.getUid(), promqlInfo, result);
						
						//결과분석
						MetricResultAnalyzer analyzer = new MetricResultAnalyzer(this.prom_nodes, this.prom_pods, this.current_millitime);
						analyzer.analyze(clusterInfo.getUid(), promqlInfo.getPrqlUid(), result);
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
				// 테스트에만 사용:문제 발생시 계속 대기할 수 있어서 운영에서는 사용하기가 부담스럽다. count 변수 적용이 필요함.
				//while(!executorService.isTerminated()) {sleep(10);}
				
				//shutdown 후에 모든 job이 종료될 동안 대기한다. 최대 10초를 대기한다.
				executorService.awaitTermination(10, TimeUnit.SECONDS);
				
				//{{API에만 있는 데이터 추가 처리.
				this.prom_nodes.getAllNodeList().forEach(k->{
					ClusterNode cnode = this.nodeApiMap.get(k.getClUid() + "_" + k.getNode());
					k.setNoUid(cnode.getUid());  //node uid 설정
					k.setStatus(cnode.getStatus());  // 상태 설정
					k.addLabels(cnode.getLabels());  // labels 추가
				});

				//}}API에만 있는 데이터 추가 처리.
				if(1 == 1) {
				}
			} catch (InterruptedException e) {
				log.error("awaitTermination Error:", e);
			}
			
			//여기서 블러킹 큐에 등록해야겠다.
			
			nodeDeque.addFirst(this.prom_nodes);
			podDeque.addFirst(this.prom_pods);
			
			
			promqlIdList.clear();
			promqlIdList = null;
		}catch(Exception e) {
			log.error("MetricWorker Thread Error:",e);
		}
	}

	private void insertResult(int cl_uid, PromQL promqlInfo, String rsJsonString) {
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

	//private String getPrometheusResult(String url, HashMap<String, String> param) throws IOException {
	private String getPrometheusResult(String url, org.jsoup.Connection.Method method, Map<String, String> params, String bodyString) throws IOException {
		
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
			log.debug("수집상태코드 {}?query={} {}", url, logQuery, dataPage.statusCode());
		}

		String json_string = dataPage.body();
		
		//별도의 로그 파일에 기록함
		if (metricLog.isInfoEnabled())
			metricLog.info("\n##Request:{}?query={}\n##Response:{}", url, logQuery, json_string);
		return json_string;
	}
}

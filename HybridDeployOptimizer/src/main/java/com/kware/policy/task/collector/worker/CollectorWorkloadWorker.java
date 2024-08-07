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
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;

import org.jsoup.Jsoup;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.kware.common.util.HashUtil;
import com.kware.common.util.HttpSSLFactory;
import com.kware.common.util.JSONUtil;
import com.kware.common.util.StringUtil;
import com.kware.policy.task.collector.service.ClusterManagerService;
import com.kware.policy.task.collector.service.vo.ClusterWorkload;
import com.kware.policy.task.collector.service.vo.ClusterWorkloadPod;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.collector.service.vo.PromMetricPods;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.constant.APIConstant;
import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.APIQueue.APIMapsName;
import com.kware.policy.task.common.queue.PromQueue;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

@Slf4j
@SuppressWarnings("rawtypes")
public class CollectorWorkloadWorker extends Thread {
	final QueueManager qm = QueueManager.getInstance();
	/*
	 * @SuppressWarnings("unchecked") ConcurrentHashMap<String, ClusterWorkload>
	 * workloadApiMap = (ConcurrentHashMap<String,
	 * ClusterWorkload>)qm.getApiMap(QueueManager.APIMapsName.WORKLOAD);
	 * 
	 * @SuppressWarnings("unchecked") ConcurrentHashMap<String, ClusterWorkloadPod>
	 * workloadPodApiMap = (ConcurrentHashMap<String,
	 * ClusterWorkloadPod>)qm.getApiMap(QueueManager.APIMapsName.WORKLOADPOD);
	 * 
	 * @SuppressWarnings("unchecked") BlockingDeque<PromMetricPods> podDeque =
	 * (BlockingDeque<PromMetricPods>)qm.getPromDeque(QueueManager.PromDequeName.
	 * METRIC_PODINFO);
	 */
	
	APIQueue apiQ   = qm.getApiQ();
	PromQueue promQ = qm.getPromQ();
	
	//JPATH
	//private final static String JPATH_API_RESULT = "$.result.content"; //파라미터를 {pageRequest{ page: 1, size: 1}} 가 있을때와 없을때 차이가 있네.
	private final static String JPATH_API_RESULT = "$.result";
	//private final static String JPATH_API_RESULT_WORKLOADS = "$.result";
	//private final static String JPATH_METRIC = "$.metric";
	//private final static String JPATH_VALUE  = "$.value";
	private final static String JPATH_CODE   = "$.code";
	private final static String JPATH_ALL    = "$..*";
	
	private final static String REG_mlId     = "\\{mlId\\}";
	
	ClusterManagerService service = null;
	boolean isRunning = false;
	//HashMap<String, Object> clusterInfo = null;
	
	private String authorization_token = null;
	private String api_base_url = null;
	
	boolean isFinishEnable = false; //외부 Property 변수 처리???
	boolean isDeleteEnable = false; //외부 Property 변수 처리???
	
	
	private final String SESSIONID   = UUID.randomUUID().toString();

	public boolean isRunning() {
		return this.isRunning;
	}

	public void setClusterManagerService(ClusterManagerService cmService) {
		this.service = cmService;
	}
	
	public void setAuthorizationToken(String authorization_token) {
		this.authorization_token = authorization_token;
	}
	
	public void setApiBaseUrl(String api_base_url) {
		this.api_base_url = api_base_url;
	}
	
	public void setPropertyForAPI(boolean _isFinish, boolean _isDelete) {
		this.isFinishEnable = _isFinish;
		this.isDeleteEnable = _isDelete;
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		this.isRunning = true;
		try {
		
			/**
			 * 1. api get clusterlist => clusterInfo
			 * 2. api get clusterDetail => ClusterNode, prometheus
			 * 3. db input or update, clusterinfo, nodeinfo 이때 삭제된 노드가 있는지 확인하고 처리해야한다.
			 */
			
			String url = this.api_base_url + APIConstant.API_ML_LIST;
			String apiResult;
			try {
				//Map<String,String> params = new HashMap<String, String>();
				
				apiResult = this.getAPIResult(url, org.jsoup.Connection.Method.POST, null, "{}");
			} catch (IOException e) {
				log.error("스레드 종료: 워크로드 리스트 API 호출 에러: {}",url, e);
				return;
			}
			
			DocumentContext list_ctx = JsonPath.parse(apiResult);
			List<Map<String, Object>> workloadList = this.annlyApiResultFromWorkloadList(list_ctx);
			
			
			Map apiWorkloadPodMap = this.apiQ.getApiWorkloadPodMap();
			Map apiWorkloadMap    = this.apiQ.getApiWorkloadMap();
			BlockingDeque<PromMetricPods> promPodsDeque   = this.promQ.getPromPodsDeque();
			
			for(Map<String,Object> mTmp: workloadList) {
				String mlId = (String)mTmp.get(StringConstant.STR_mlId);
				
				url = this.api_base_url + APIConstant.API_ML;
				url = url.replaceAll(REG_mlId, mlId);
				
				//{{ 이부분을 스레드로 변경이 가능함
				try {
					apiResult = this.getAPIResult(url, org.jsoup.Connection.Method.GET, null, null);
				} catch (IOException e) {
					log.error("스레드 종료: 워크로드 상세 API 호출 에러: {}",url, e);
					return;
				}
				
				DocumentContext detail_ctx = JsonPath.parse(apiResult);
				Map<String, Object> workloadDetailMap = this.annlyApiResultFromWorkloadDetail(detail_ctx);
				
				//{{DB입력
				ClusterWorkload workload = this.insertClusterWorkload(mlId, workloadDetailMap, apiWorkloadMap);
				//}}
				
				//{{전역 큐에 등록
				workload.setSessionId(SESSIONID);
				//{{ workload에 속해있는 파드 리스트 저장
				Map<String, ClusterWorkloadPod> pods = this.getPodsFromDetail(workloadDetailMap);
				workload.setMPods(pods);
				
				//{{ ---------------여기에서 pods의 리스트들의 완료여부를 확인해서 모두 완료상태이면 종료API 발신한다.
				/**
				 * api에서 수집한 워크로드 상태가 finished가 아니면 프로메테우스에서 검색해서 
				 *   - 없는 것은 현재 처리하지 않음(나중에 제거할 필요 있음)
				 *   - 있는데 종료되었으면 종료 api호출
				 *   
				 *  api에서 수집한 워크로드 상태가 finished면 
				 *    - 삭제 API 호출
				 *    
				 *    종료가 이루어지고 다음 수집 시간에 삭제가 이루어지는 구조임
				 *    
				 * ===================================== 향후 처리 ===========================================
				 * api에 존재하는 워크로드와 파드의 시작시간을 등록하고, 
				 * 시작시간이 1시간이 되었는데도 진행하지 않으면, 삭제처리할까? 아니 이부분은 자동으로 처리하지 않는 것으로 하고
				 * 시간은은 시작시간 갱신시간을 Long 값으로 일단 관리하자, DB에선 json에 있으므로 별도로 저장하지 않는다.
				 * =========================================================================================
				 */
				try { //전체 업무에 방해받지 않도록하기 위함
					if(isFinishEnable && workload.getDeleteAt().equals(StringConstant.STR_N)) { //상태가 미완료: finished가 아니면
						boolean isCompleteWorkload = true;
						PromMetricPods mPods = promPodsDeque.peek();
						for (Map.Entry<String, ClusterWorkloadPod> entry : pods.entrySet()) {
						    PromMetricPod mPod = mPods.getMetricPod(workload.getClUid(), entry.getValue().getUid());
						    if(mPod == null) { 
						    	isCompleteWorkload = false;
						    }else if (!mPod.isCompleted()) {
						        isCompleteWorkload = false;
						    }
						}
						
						//프로메테우스에서는 완료되었다고 나온다.
						if(isCompleteWorkload) {
							this.finishApiForWorklaod(mlId); // 완료처리
						}
					}else if(isDeleteEnable && workload.getDeleteAt().equals(StringConstant.STR_Y)) { //완료되었으면 삭제처리
						this.deleteApiForWorklaod(mlId);
					}
				}catch(Exception e) {
					log.error("API 호출 error", e);
				}
				//}}-----------------------------
				
				//별도의 map에 podUid를 통해서 mlId를 찾을 수 있도록 등록함
				apiWorkloadPodMap.putAll(pods);
				//}}파드리스트
				apiWorkloadMap.put(mlId, workload);
				//}} 전역큐 등록
				
				detail_ctx.delete(JPATH_ALL);
				workloadDetailMap.clear();
				workloadDetailMap = null;
				//}} 스레드 변경 가능함
			}
			
			list_ctx.delete(JPATH_ALL);
			
			workloadList.clear();
			workloadList = null;
			
			//여기서 SESSIONID와 비교해서 제거해야한다. this.workloadMap:: 기존에 수집했는데, 이번에는 수집되지 않는 데이터 제거
			//Map에 사용하는 객체는 모두 ClusterDefault를 상속받아서 사용해야 정상작동한다.
			apiQ.removeNotIfSessionId(APIMapsName.WORKLOAD   , SESSIONID);
			apiQ.removeNotIfSessionId(APIMapsName.WORKLOADPOD, SESSIONID);
	
			if(log.isDebugEnabled())
				log.debug("current {} maps size={}", APIMapsName.WORKLOAD.toString(), apiQ.getApiQueueSize(APIMapsName.WORKLOAD));
		}catch(Exception e) {
			log.error("WorkloadWorker Thread Error:" , e);
		}

	}
	
	private List<Map<String, Object>> annlyApiResultFromWorkloadList(DocumentContext _ctx) {
		List<Map<String, Object>> resultList = null;

		String code = _ctx.read(JPATH_CODE); 		// 정상상태인지 code 값 확인
		if (APIConstant.API_RESULT_CODE_OK.equals(code)) {
			resultList = _ctx.read(JPATH_API_RESULT);
//			resultList = (List)SerializationUtils.clone((ArrayList)resultList);
		} else {
			log.error("에러: code가 10001이 아닙니다.");
		}
		
		return resultList;
	}
	
	private Map<String, Object> annlyApiResultFromWorkloadDetail(DocumentContext _ctx) {
		Map<String, Object> resultMap = null;
		
		// code 값 확인
		String code = _ctx.read(JPATH_CODE);
		if (APIConstant.API_RESULT_CODE_OK.equals(code)) {
			resultMap = _ctx.read(JPATH_API_RESULT);
//			resultMap = (Map)SerializationUtils.clone((HashMap)resultMap);
		} else {
			log.error("에러: code가 10001이 아닙니다.");
		}
		
		return resultMap;
	}


	/**
	 * worklaod detail정보에서 pods관련 정보를 분리하는 함수
	 * @return
	 */
	private Map<String, ClusterWorkloadPod> getPodsFromDetail(Map<String, Object> resultMap){
		Map<String, ClusterWorkloadPod> wpMap = new HashMap<String, ClusterWorkloadPod>();	
		String mlId = (String)resultMap.get(StringConstant.STR_mlId);
				
		JSONArray resourcesArray = (JSONArray)resultMap.get(StringConstant.STR_resources);
		for(int i = 0 ; i < resourcesArray.size(); i++) {
			Map resource = (Map)resourcesArray.get(i);
			JSONArray podsArray = (JSONArray)resource.get(StringConstant.STR_pods);
			for(int j = 0 ; j < podsArray.size(); j++) {
				Map pod = (Map)podsArray.get(j);
				ClusterWorkloadPod wPod = new ClusterWorkloadPod();
				wPod.setUid((String)pod.get(StringConstant.STR_uid));
				wPod.setKind((String)pod.get(StringConstant.STR_kind));
				wPod.setNode((String)pod.get(StringConstant.STR_node));
				wPod.setPod((String)pod.get(StringConstant.STR_pod));
				
				String sCreatedAt = (String)pod.get(StringConstant.STR_createdAt);
				String sUpdatedAt = (String)pod.get(StringConstant.STR_updatedAt);
				
				wPod.setCreatedAt(StringUtil.getMilliseconds(formatter, sCreatedAt));
				wPod.setUpdatedAt(StringUtil.getMilliseconds(formatter, sUpdatedAt));
				
				wPod.setMlId(mlId);
				wPod.setSessionId(SESSIONID);
				wpMap.put(wPod.getUid(), wPod);
			}
		}
		
		return wpMap;
	}
	
	final static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
	
	private ClusterWorkload insertClusterWorkload(String _ml_uid, Map<String, Object> _workloadMap, Map<String, Object> _apiWorkloadMap) {
		ClusterWorkload workload = new ClusterWorkload();
		Integer nId    = (Integer)_workloadMap.get(StringConstant.STR_id);
		String sStatus = (String)_workloadMap.get(StringConstant.STR_status);
		String sCreatedAt = (String)_workloadMap.get(StringConstant.STR_createdAt);
		String sUpdatedAt = (String)_workloadMap.get(StringConstant.STR_updatedAt);
		
		workload.setCreatedAt(StringUtil.getMilliseconds(formatter, sCreatedAt));
		workload.setUpdatedAt(StringUtil.getMilliseconds(formatter, sUpdatedAt));
		
		workload.setMlId(_ml_uid);
		workload.setId(nId);
		//workload.setClUid(_cl_uid); //정보가 없어서 메트릭에서 추가해야한다.
		if(StringConstant.STR_finished.equalsIgnoreCase(sStatus)) {  //Started | finished
			workload.setDeleteAt(StringConstant.STR_Y);
		}else {
			workload.setDeleteAt(StringConstant.STR_N);
		}
		workload.setNm((String)_workloadMap.get(StringConstant.STR_name));
		workload.setMemo((String)_workloadMap.get(StringConstant.STR_description));
		workload.setUserId((String)_workloadMap.get(StringConstant.STR_userId));
		
		String sJson = JSONUtil.getJsonStringFromMap(_workloadMap);
		String sHashVal = HashUtil.getMD5Hash(sJson);
		
		workload.setInfo(sJson);
		workload.setHashVal(sHashVal);
		
		try {
			//기존에 이미 등록되어 있으면. 
			ClusterWorkload t = (ClusterWorkload)_apiWorkloadMap.get(_ml_uid);
			if(t != null) {
				workload.setClUid(t.getClUid());
				this.service.updateClusterWorkloadAndInsertHistory(workload);
			}else {				
				this.service.insertClusterWorkload(workload);
			}
		} catch (Exception e) {
			log.error("데이터 Insert 에러 Wrokload:\n{}", workload, e);
		}
		
		return workload;
	}
	
	/**
	 * 워크로드 종료하는 API 호출
	 * @param _mlId
	 * @return
	 * @throws IOException
	 */
	private Boolean finishApiForWorklaod(String _mlId) throws IOException {
		String url = this.api_base_url + APIConstant.API_ML_FINISH;
		url = url.replaceAll(REG_mlId, _mlId);
		
		String apiResult = this.getAPIResult(url, org.jsoup.Connection.Method.PUT, null, null);
		DocumentContext detail_ctx = JsonPath.parse(apiResult);
		// code 값 확인
		String code = detail_ctx.read(JPATH_CODE);
		if (APIConstant.API_RESULT_CODE_OK.equals(code)) {
			detail_ctx.delete(JPATH_ALL);
			return true;	
		} else {
			log.error("에러: code가 10001이 아닙니다.");
			return false;
		}
	}

	/**
	 * 워크로드 삭제하는 API 호출
	 * @param _mlId
	 * @return
	 * @throws IOException
	 */
	private Boolean deleteApiForWorklaod(String _mlId) throws IOException {
		String url = this.api_base_url + APIConstant.API_ML_DELETE;
		String bodyString = "{ \"mlId\": \"" + _mlId + "\"	}";
		String apiResult = this.getAPIResult(url, org.jsoup.Connection.Method.DELETE, null, bodyString);
		DocumentContext detail_ctx = JsonPath.parse(apiResult);
		// code 값 확인
		String code = detail_ctx.read(JPATH_CODE);
		if (APIConstant.API_RESULT_CODE_OK.equals(code)) {
			detail_ctx.delete(JPATH_ALL);
			return true;	
		} else {
			log.error("에러: code가 10001이 아닙니다.");
			return false;
		}
	}
	
	//private String getPrometheusResult(String url, HashMap<String, String> param) throws IOException {
	private String getAPIResult(String url, org.jsoup.Connection.Method method, Map<String, String> params, String bodyString) throws IOException {
		org.jsoup.Connection connection = Jsoup.connect(url)
				.method(method)
				.timeout(30 * 1000)
				.followRedirects(true)
				// .validateTLSCertificates(false)
				.sslSocketFactory(HttpSSLFactory.socketFactory())
				.ignoreContentType(true);
		
		if(params != null) {
			connection.data(params);
		}
		
		if(bodyString != null) {
			connection.header(StringConstant.STR_Content_Type, StringConstant.STR_application_json);
			connection.requestBody(bodyString);
		}
		
		if(this.authorization_token != null) {
			connection.header(StringConstant.STR_Authorization, this.authorization_token);
		}
		
		org.jsoup.Connection.Response dataPage = connection.execute();
		
		if (log.isDebugEnabled()) {
			log.debug("수집상태코드 {}, params={} {}", url, params, dataPage.statusCode());
		}

		String json_string = dataPage.body();
		if (log.isInfoEnabled())
			log.info("\n##Request:{}, params={}\n##Response:{}", url, params, json_string);
		
		return json_string;
	}
}

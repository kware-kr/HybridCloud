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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.kware.policy.task.collector.service.vo.ClusterWorkloadResource;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.collector.service.vo.PromMetricPods;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.WorkloadCommandManager;
import com.kware.policy.task.common.constant.APIConstant;
import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.APIQueue.APIMapsName;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.PromQueue.PromDequeName;
import com.kware.policy.task.common.queue.RequestQueue;
import com.kware.policy.task.common.queue.WorkloadContainerQueue;
import com.kware.policy.task.common.service.CommonService;
import com.kware.policy.task.common.vo.WorkloadCommand;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadTaskWrapper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

@Slf4j
@SuppressWarnings("rawtypes")
public class CollectorWorkloadApiWorker extends Thread {
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
	
	APIQueue     apiQ     = qm.getApiQ();
	PromQueue    promQ    = qm.getPromQ();
	RequestQueue requestQ = qm.getRequestQ();
	
	//JPATH
	private final static String JPATH_API_RESULT_CONTENT = "$.result.content"; //파라미터를 {pageRequest{ page: 1, size: 1}} 가 있을때와 없을때 차이가 있네.
	private final static String JPATH_API_RESULT         = "$.result";
	//private final static String JPATH_API_RESULT_WORKLOADS = "$.result";
	//private final static String JPATH_METRIC = "$.metric";
	//private final static String JPATH_VALUE  = "$.value";
	private final static String JPATH_CODE   = "$.code";
	private final static String JPATH_ALL    = "$..*";
	
	//pageable
	private final static String JPATH_last           = "$.result.last";
	private final static String JPATH_totlaElement   = "$.result.totalElements";
	
	private final static String REG_mlId     = "\\{mlId\\}";
	
	ClusterManagerService service = null;
	CommonService comService = null;
	
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
	
	public void setCommonService(CommonService comService) {
		this.comService = comService;
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

	
	@Data
	public static class WorkloadAPIRequest{
		int page;
		int size = 20;
		//String status;
	}
	
	boolean isFirst = false; //처음 실행인지 여부확인, api가 없으면

	
	public void setIsFirst(boolean isFirst) {
		this.isFirst = isFirst;
	}
	
	Map apiWorkloadPodMap = this.apiQ.getApiWorkloadPodMap();
	Map apiWorkloadMap    = this.apiQ.getApiWorkloadMap();
	
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
			
			DocumentContext list_ctx = null;
			List<Map<String, Object>> workloadList = new ArrayList<Map<String, Object>>();
			int pageNumber    = 0;
			int totalElements = 0;
			int currentElements = 0;
			boolean isLast = false;
			while(!isLast) {
				pageNumber++;
				try {
					WorkloadAPIRequest pageRequest = new WorkloadAPIRequest();
					pageRequest.page = pageNumber;
					
					Map<String, WorkloadAPIRequest> bodyparm  = new HashMap<String, WorkloadAPIRequest>();
					bodyparm.put("pageRequest", pageRequest);
					
					String bodyString = JSONUtil.getJsonStringFromMap(bodyparm);
					bodyparm.clear();
					bodyparm = null;
					
					apiResult = this.getAPIResult(url, org.jsoup.Connection.Method.POST, null, bodyString);
					//apiResult = this.getAPIResult(url, org.jsoup.Connection.Method.POST, null, "{}");
					
				} catch (IOException e) {
					log.error("스레드 종료: 워크로드 리스트 API 호출 에러: {}",url, e);
					return;
				}
				
				list_ctx = JsonPath.parse(apiResult);
				
				//pageabel를 사용하므로 result.content를 가져옴
				List<Map<String, Object>> workloadList_temp = this.annlyApiResultFromWorkloadList(list_ctx);
				if(workloadList_temp == null)
					break;
				
				workloadList.addAll(workloadList_temp);
				currentElements = workloadList_temp.size(); 
				workloadList_temp.clear();
				
				isLast = list_ctx.read(JPATH_last);
				totalElements = list_ctx.read(JPATH_totlaElement);
				
				if(isLast == false) {
					if(totalElements <= currentElements) // 혹시 못빠져나놀가봐
						break;
				}
			}
			
			
			//Map apiWorkloadPodMap = this.apiQ.getApiWorkloadPodMap();
			//Map apiWorkloadMap    = this.apiQ.getApiWorkloadMap();
			
			for(Map<String,Object> mTmp: workloadList) {
				String mlId = (String)mTmp.get(StringConstant.STR_mlId);
				
				if(mlId == null)
					continue;
				
				url = this.api_base_url + APIConstant.API_ML;
				url = url.replaceAll(REG_mlId, mlId); //mlId가 null이면 오류
				
				//{{ 이부분을 스레드로 변경이 가능함
				//워크로드 상세 정보 조회
				try {
					apiResult = this.getAPIResult(url, org.jsoup.Connection.Method.GET, null, null);
				} catch (IOException e) {
					log.error("스레드 종료: 워크로드 상세 API 호출 에러: {}",url, e);
					return;
				}
				
				DocumentContext detail_ctx = JsonPath.parse(apiResult);
				Map<String, Object> workloadDetailMap = this.annlyApiResultFromWorkloadDetail(detail_ctx);
				if(workloadDetailMap == null)
					continue;
				
				//{{DB입력
				ClusterWorkload workload = this.makeClusterWorkload(mlId, workloadDetailMap, this.apiWorkloadMap);
				ClusterWorkload oldWorkload = (ClusterWorkload)this.apiWorkloadMap.get(workload.getUniqueKey());
				
				//{{RequestMap에 생성일자 등록
				if(workload.getCreatedAt() != null) {
					WorkloadRequest  workloadRequest = requestQ.getWorkloadRequest(mlId);
					if(workloadRequest != null) {
						if(workloadRequest.getRequest().getDeployedAt() == null) {
							workloadRequest.getRequest().setDeployedAt(workload.getCreatedAt().toLocalDateTime());		
						}
					}
				}
				//}} RequestMap에 생성일자 등록
				
				//{{전역 큐에 등록
				workload.setSessionId(SESSIONID);
				//{{ workload에 속해있는 리소스와 파드 리스트 파드 리스트 저장
				this.makeResourceAndPodsFromDetail(workload, workloadDetailMap);
				//workload.setResourcePodClUidAll();
			
				this.insertWorklod(oldWorkload, workload);//DB 입력					
				//}}
				
				 
				
				
				//{{ ---------------여기에서 pods의 리스트들의 완료여부를 확인해서 모두 완료상태이면 종료API 발신한다.
				/**
				 * api에서 수집한 워크로드 상태가 finished가 아니면 프로메테우스에서 검색해서 
				 *   - 없는 것은 현재 처리하지 않음(나중에 제거할 필요 있음)
				 *   - 있는데 종료되었으면 종료 api호출
				 *   완료체크는 각 파드의 completedTimestamp가 존재하는 경우에만 해당 파드가 완료된 것으로 파악하고, 
				 *   워크로드가 Error 상태인 것은 처리하지 않는다.(이건 포탈에서)
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
						PromMetricPods mPods = (PromMetricPods)promQ.getPromDequesFirstObject(PromDequeName.METRIC_PODINFO);
						if(mPods != null) {
							for(Map.Entry<String, ClusterWorkloadResource> resourceE : workload.getResourceMap().entrySet() ) {
								Map<String, ClusterWorkloadPod> podMap = resourceE.getValue().getPodMap();
								
								//워크로드 api갯수와 requesqQ의 container 갯수가 틀리면 처리하지 않는다.
								int request_container_cnt = this.requestQ.getWorkloadContainerSize(mlId);
								if(request_container_cnt <= 0) { // 워크로드 정보가 없는 상태
									isCompleteWorkload = false;
									continue;
								}
								
								int api_pod_cnt = podMap.size();
								if(api_pod_cnt < request_container_cnt) { //최소한 같거나 더 많으면..
									isCompleteWorkload = false;
									continue;
								}
								
								for (Map.Entry<String, ClusterWorkloadPod> entry : podMap.entrySet()) {
									String wPodUid = null;
									ClusterWorkloadPod wPod =  entry.getValue();
									if(wPod != null)
										wPodUid = wPod.getUid();
									
								    PromMetricPod mPod = mPods.getMetricPod(workload.getClUid(), wPodUid);
								    if(mPod == null) { 
								    	isCompleteWorkload = false; 
								    	break;
								    }else if (!mPod.isCompleted()) {
								        isCompleteWorkload = false;
								        break;
								    }
								}
								
								//한개라도 false면
								if(!isCompleteWorkload)
									break;
							}
						}else {
							isCompleteWorkload = false;
						}
						
						//프로메테우스에서는 완료되었다고 나온다.
						if(isCompleteWorkload) {
							if(this.finishApiForWorklaod(mlId)) { // 완료처리
								WorkloadCommand<String> command = new WorkloadCommand<String>(WorkloadCommand.CMD_WLD_COMPLETE,mlId);
								WorkloadCommandManager.addCommand(command);
							}
						}
					}else if(isDeleteEnable && workload.getDeleteAt().equals(StringConstant.STR_Y)) { //완료되었으면 삭제처리
						this.deleteApiForWorklaod(mlId);
					}
				}catch(Exception e) {
					log.error("API 호출 error", e);
				}
				//}}-----------------------------
				
				//별도의 map에 podUid를 통해서 mlId를 찾을 수 있도록 등록함
				for(Map.Entry<String, ClusterWorkloadResource> resourceE : workload.getResourceMap().entrySet() ) {
					Map<String, ClusterWorkloadPod> podMap = resourceE.getValue().getPodMap();
					apiWorkloadPodMap.putAll(podMap);
				}
				//}}파드리스트
				this.apiWorkloadMap.put(mlId, workload);
				//}} 전역큐 등록
				
				detail_ctx.delete(JPATH_ALL);
				workloadDetailMap.clear();
				workloadDetailMap = null;
				//}} 스레드 변경 가능함
			}//for
			
			if(list_ctx != null)
				list_ctx.delete(JPATH_ALL);
			
			workloadList.clear();
			workloadList = null;
			
			//여기서 SESSIONID와 비교해서 제거해야한다. this.workloadMap:: 기존에 수집했는데, 이번에는 수집되지 않는 데이터 제거
			//Map에 사용하는 객체는 모두 ClusterDefault를 상속받아서 사용해야 정상작동한다.
			HashMap removedMap = null;
			removedMap = apiQ.removeNotIfSessionId(APIMapsName.WORKLOAD   , SESSIONID);
			delete(removedMap);//앞 처리과정에서 완료되어도 삭제하지만, 큐에서 제거되는 경우에도 DB 삭제
			removedMap.clear();
			
			removedMap = apiQ.removeNotIfSessionId(APIMapsName.WORKLOADPOD, SESSIONID);
			removedMap.clear();
	
			if(log.isDebugEnabled())
				log.debug("current {} maps size={}", APIMapsName.WORKLOAD.toString(), apiQ.getApiQueueSize(APIMapsName.WORKLOAD));
		}catch(Exception e) {
			log.error("WorkloadWorker Thread Error:" , e);
		}

	}
	
	/**DB 삭제
	 */
	private void delete(HashMap<String, Object> _removedMap) {
		ClusterWorkload wl = null;
		for(Object obj: _removedMap.values()) {
			if(obj instanceof ClusterWorkload) {
				wl = (ClusterWorkload)obj;
				this.service.deleteClusterWorkload(wl);

				//WraperQueue에서 제거
				WorkloadCommand<String> command = new WorkloadCommand<String>(WorkloadCommand.CMD_WLD_COMPLETE,wl.getMlId());
				WorkloadCommandManager.addCommand(command);				
			}
		}
	}
	
	private List<Map<String, Object>> annlyApiResultFromWorkloadList(DocumentContext _ctx) {
		List<Map<String, Object>> resultList = null;

		String code = _ctx.read(JPATH_CODE); 		// 정상상태인지 code 값 확인
		if (APIConstant.API_RESULT_CODE_OK.equals(code)) {
			resultList = _ctx.read(JPATH_API_RESULT_CONTENT);
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
	
	final static SimpleDateFormat formatterpod = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	final static SimpleDateFormat formatter    = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
	
	private ClusterWorkload makeClusterWorkload(String _ml_uid, Map<String, Object> _workloadMap, Map<String, Object> _apiWorkloadMap) {
		ClusterWorkload workload = new ClusterWorkload();
		Integer nId       = (Integer)_workloadMap.get(StringConstant.STR_id);
		String sStatus    = (String)_workloadMap.get(StringConstant.STR_status);
		String sCreatedAt = (String)_workloadMap.get(StringConstant.STR_createdAt);
		String sUpdatedAt = (String)_workloadMap.get(StringConstant.STR_updatedAt);

		
		Integer nClusterIdx = (Integer)_workloadMap.get(StringConstant.STR_clusterIdx); // clustertId 추가 요청으로 인해 2024 추가
		
		workload.setMlId(_ml_uid);
		workload.setId(nId);
		//workload.setClUid(_cl_uid); //정보가 없어서 메트릭에서 추가해야한다.:MetricResultAnalyzer
	    workload.setClUid(nClusterIdx);

		
		workload.setCreatedAt(StringUtil.getTimestamp(formatter, sCreatedAt));
		workload.setUpdatedAt(StringUtil.getTimestamp(formatter, sUpdatedAt));
		
		//workload.setCreatedAt(StringUtil.getMilliseconds(formatter, sCreatedAt));
		//workload.setUpdatedAt(StringUtil.getMilliseconds(formatter, sUpdatedAt));
		

		
		workload.setStatusString(sStatus);
		if(StringConstant.STR_finished.equalsIgnoreCase(sStatus)) {  //Started | finished
			workload.setDeleteAt(StringConstant.STR_Y);
		}else {
			workload.setDeleteAt(StringConstant.STR_N);
		}
		workload.setNm(       (String)_workloadMap.get(StringConstant.STR_name));
		workload.setMemo(     (String)_workloadMap.get(StringConstant.STR_description));
		workload.setUserId(   (String)_workloadMap.get(StringConstant.STR_userId));
		workload.setNamespace((String)_workloadMap.get(StringConstant.STR_namespace));
		
		String sJson = JSONUtil.getJsonStringFromMap(_workloadMap);
		String sHashVal = HashUtil.getMD5Hash(sJson);
		
		workload.setInfo(sJson);
		workload.setHashVal(sHashVal);
		
		
		return workload;
	}
	
	//DB입력
	private void insertWorklod(ClusterWorkload oldWorkload, ClusterWorkload curWorkload) {
		try {
			//기존에 이미 등록되어 있으면. 
			//ClusterWorkload oldObj = (ClusterWorkload)_apiWorkloadMap.get(workload.getUniqueKey());
			if(oldWorkload != null) {
				curWorkload.setClUid(oldWorkload.getClUid());
				//if(!oldWorkload.getHashVal().equals(cruWorkload.getHashVal()) || oldWorkload.getClUid() != cruWorkload.getClUid())
				this.service.updateClusterWorkloadAndInsertHistory(curWorkload);
			}else {
				oldWorkload = null;
				if(this.isFirst) {
					oldWorkload = this.service.selectClusterWorkload(curWorkload);
				}
				if(oldWorkload == null) {				
					this.service.insertClusterWorkload(curWorkload);
					//websocket
					this.service.sendNewClousterWorkload(curWorkload.getMlId());
				}else {
					this.service.updateClusterWorkload(curWorkload);
				}
			}
		} catch (Exception e) {
			log.error("데이터 Insert 에러 Wrokload:\n{}", curWorkload, e);
		}
		
	}
	
	/**
	 * worklaod detail정보에서 pods관련 정보를 분리하는 함수
	 * @return
	 */
	private void makeResourceAndPodsFromDetail(ClusterWorkload _workload, Map<String, Object> _detailResultMap){
		String mlId = (String)_detailResultMap.get(StringConstant.STR_mlId);
				
		ClusterWorkloadResource cwResource = null;
		JSONArray resourcesArray = (JSONArray)_detailResultMap.get(StringConstant.STR_resources);
		Map<String, ClusterWorkloadPod> wpMap = null;
		for(int i = 0 ; i < resourcesArray.size(); i++) {
			Map resource = (Map)resourcesArray.get(i);
			
			cwResource = new ClusterWorkloadResource();
			cwResource.setClUid(_workload.getClUid());
			cwResource.setKind((String)resource.get(StringConstant.STR_kind));
			cwResource.setNm(  (String)resource.get(StringConstant.STR_name));
			cwResource.setUid( (String)resource.get(StringConstant.STR_uid));
			
			cwResource.setStatus(         (String)resource.get(StringConstant.STR_status));
			cwResource.setId(             (Integer)resource.get(StringConstant.STR_id));
			cwResource.setTotalPodCount(  (Integer)resource.get(StringConstant.STR_totalPodCount));
			cwResource.setRunningPodCount((Integer)resource.get(StringConstant.STR_runningPodCount));
			
			String sCreatedAt = (String)resource.get(StringConstant.STR_createdAt);
			String sUpdatedAt = (String)resource.get(StringConstant.STR_updatedAt);
			
			cwResource.setCreatedAt(StringUtil.getTimestamp(sCreatedAt));
			cwResource.setUpdatedAt(StringUtil.getTimestamp(sUpdatedAt));
			
			
			JSONArray podsArray = (JSONArray)resource.get(StringConstant.STR_pods);
			wpMap = new HashMap<String, ClusterWorkloadPod>();
			for(int j = 0 ; j < podsArray.size(); j++) {
				Map pod = (Map)podsArray.get(j);
				ClusterWorkloadPod wPod = new ClusterWorkloadPod();
				
				wPod.setResourceName(cwResource.getNm());
				wPod.setClUid(_workload.getClUid());
				wPod.setUid((String)pod.get(StringConstant.STR_uid));
				wPod.setKind((String)pod.get(StringConstant.STR_kind));
				wPod.setNode((String)pod.get(StringConstant.STR_node));
				wPod.setPod((String)pod.get(StringConstant.STR_name));
				wPod.setStatus((String)pod.get(StringConstant.STR_status));
				
				wPod.setNamespace((String)pod.get(StringConstant.STR_namespace));
				wPod.setOwnerUid( (String)pod.get(StringConstant.STR_ownerUid));
				wPod.setOwnerName((String)pod.get(StringConstant.STR_ownerName));
				wPod.setOwnerKind((String)pod.get(StringConstant.STR_ownerKind));
				wPod.setRestart( (Integer)pod.get(StringConstant.STR_restart));
				
				sCreatedAt = (String)pod.get(StringConstant.STR_createdAt);
				sUpdatedAt = (String)pod.get(StringConstant.STR_updatedAt);
				
				wPod.setCreatedAt(StringUtil.getTimestamp(sCreatedAt));
				wPod.setUpdatedAt(StringUtil.getTimestamp(sUpdatedAt));
				
				//wPod.setCreatedAt(StringUtil.getMilliseconds(formatterpod, sCreatedAt));
				//wPod.setUpdatedAt(StringUtil.getMilliseconds(formatterpod, sUpdatedAt));
				
				wPod.setMlId(mlId);
				wPod.setSessionId(SESSIONID);
				wpMap.put(wPod.getUid(), wPod);
			}
			
			//동일한 owner를 기반으로 등록된다.
			_workload.addResource(cwResource);
			cwResource.setPodMap(wpMap);			
		}
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
			
			this.comService.createEvent("Workload 완료", "Workload"
					, "워크로드 모든 TASK 종료 및 종료 API 호출.\n" + _mlId);
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
			
			this.comService.createEvent("Workload 삭제", "Workload"
					, "워크로드 종료 상태 확인 및 삭제 API 호출.\n" + _mlId);
			return true;	
		} else {
			log.error("에러: code가 10001이 아닙니다.");
			return false;
		}
	}
	
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

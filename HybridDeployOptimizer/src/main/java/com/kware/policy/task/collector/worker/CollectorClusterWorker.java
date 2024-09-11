package com.kware.policy.task.collector.worker;  

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jsoup.Jsoup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.kware.common.util.HashUtil;
import com.kware.common.util.HttpSSLFactory;
import com.kware.common.util.JSONUtil;
import com.kware.common.util.StringUtil;
import com.kware.policy.task.collector.service.ClusterManagerService;
import com.kware.policy.task.collector.service.vo.Cluster;
import com.kware.policy.task.collector.service.vo.ClusterNode;
import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.collector.service.vo.PromMetricNodeGPU;
import com.kware.policy.task.collector.service.vo.PromMetricNodes;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.constant.APIConstant;
import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.APIQueue.APIMapsName;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.PromQueue.PromDequeName;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;


/**
 * portal api를 호출
 */

@Slf4j
@SuppressWarnings({"rawtypes","unchecked"})
public class CollectorClusterWorker extends Thread {
	
	final QueueManager qm = QueueManager.getInstance();
	APIQueue apiQ   = qm.getApiQ();
	PromQueue promQ = qm.getPromQ();
	
	//JPATH
	private final static String JPATH_API_RESULT       = "$.result";
	private final static String JPATH_API_RESULT_NODES = "$.result.nodes";
	//private final String JPATH_METRIC = "$.metric";
	//private final String JPATH_VALUE  = "$.value";
	private final static String JPATH_CODE   = "$.code";
	private final static String JPATH_ALL    = "$..*";
	
	
	private final String reg_clusterIdx = "\\{clusterIdx\\}";
	private final String url_graph = "/graph";
	
	ClusterManagerService service = null;
	boolean isRunning = false;
	//HashMap<String, Object> clusterInfo = null;
	
	private String authorization_token = null;
	private String api_base_url = null;
	
	
	private final String SESSIONID   = UUID.randomUUID().toString();
	
	//json 변환시 필요없는 항목을 제거하기위함
	private final Set<String> gpuFilterSet = new HashSet<String>(); 
	{
		Collections.addAll(gpuFilterSet, "temp", "availableMemory");
	}
	
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
	

	boolean isFirst = false; //처음 실행인지 여부확인, api가 없으면
	
	public void setIsFirst(boolean isFirst) {
		this.isFirst = isFirst;
	}
	
	@Override
	public void run() {
		this.isRunning = true;

		try {
			
			/**
			 * 1. api get clusterlist => clusterInfo
			 * 2. api get clusterDetail => ClusterNode, prometheus
			 * 3. db input or update, clusterinfo, nodeinfo 이때 삭제된 노드가 있는지 확인하고 처리해야한다.
			 */
			
			String url = this.api_base_url + APIConstant.API_CLUSTER_LIST;
			String apiResult;
			try {
				apiResult = this.getAPIResult(url, org.jsoup.Connection.Method.GET, null, null);
			} catch (IOException e) {
				log.error("스레드 종료: 클러스터 리스트 API 호출 에러: {}",url, e);
				return;
			}
			
			DocumentContext list_ctx = JsonPath.parse(apiResult);
			
			List<Map<String, Object>> clusterList = this.annlyApiResultFromClusterList(list_ctx);
			
			for(Map<String,Object> mTmp: clusterList) {
				Integer clusterIdx = (int)mTmp.get("clusterIdx");
				
				url = this.api_base_url + APIConstant.API_CLUSTER;
				url = url.replaceAll(this.reg_clusterIdx, clusterIdx.toString());
				
				//{{ 갯수가 너무 많으면 스레드로 변경 가능한 부분
				try {
					apiResult = this.getAPIResult(url, org.jsoup.Connection.Method.GET, null, null);
				} catch (IOException e) {
					log.error("스레드 종료: 클러스터 상세 API 호출 에러: {}",url, e);
					return;
				}
				
				DocumentContext detail_ctx = JsonPath.parse(apiResult);
				List<Map<String, Object>> clusterDetailList = this.annlyApiResultFromClusterDetail(detail_ctx);
				this.supplementCluster(detail_ctx, mTmp); //상세정보의 내용을 리스트를 통해 얻은 쪽에 보완 처리함
				
				//DB입력
				//queue clusterApiMap 클러스터 입력
				Cluster cluster = this.insertCluster(clusterIdx, mTmp);
				
				//{{노드 입력
				if(clusterDetailList != null) {
					for(Map<String, Object> mNode: clusterDetailList) {
						//DB 입력
						//queue nodeApiMap 노드 입력 
						this.insertClusterNode(cluster, mNode);
					}
					//}}노드 입력
					clusterDetailList.clear();
					clusterDetailList = null;
				}
				
				detail_ctx.delete(JPATH_ALL);
				//}} 스레드 변경 가능
			}
			
			list_ctx.delete(JPATH_ALL);
			clusterList.clear();
			clusterList = null;
			
			//여기서 SESSIONID와 비교해서 제거해야한다. this.nodeMap, this.clusterMap:: 기존에 수집했는데, 이번에는 수집되지 않는 데이터 제거
			//Map에 사용하는 객체는 모두 ClusterDefault를 상속받아서 사용해야 정상작동한다.
			HashMap removedMap = null; //앞 처리과정에서 상태를 확인해서 삭제하지만, 큐에서 제거되는 경우에도 DB 삭제
			removedMap = apiQ.removeNotIfSessionId(APIMapsName.CLUSTER, SESSIONID );
			delete(removedMap);
			removedMap.clear();
			
			removedMap = apiQ.removeNotIfSessionId(APIMapsName.NODE, SESSIONID);
			delete(removedMap);
			removedMap.clear();
			
			if(log.isDebugEnabled()) {
				log.debug("current {} map size={}", APIMapsName.CLUSTER.toString(), apiQ.getApiQueueSize(APIMapsName.CLUSTER));
				log.debug("current {} map size={}", APIMapsName.NODE.toString()   , apiQ.getApiQueueSize(APIMapsName.NODE));
			}
		}catch(Exception e) {
			log.error("ClusterWorkerThread Error:", e);
		}
	}
	
	/**
	 * DB에서 삭제함
	 * @param _removedMap
	 */
	private void delete(HashMap<String, Object> _removedMap) {
		
		for(Object obj: _removedMap.values()) {
			if(obj instanceof Cluster) {
				this.service.deleteCluster((Cluster)obj);
			}else if(obj instanceof ClusterNode) {
				this.service.deleteClusterNode((ClusterNode)obj);
			}
		}		
	}
	
	/**
	 * Cluster List분석
	 * @param _ctx
	 * @return
	 */
	private List<Map<String, Object>> annlyApiResultFromClusterList(DocumentContext _ctx) {
		List<Map<String, Object>> resultList = null;

		String code = _ctx.read(CollectorClusterWorker.JPATH_CODE); 		// 정상상태인지 code 값 확인
		if (APIConstant.API_RESULT_CODE_OK.equals(code)) {
			resultList = _ctx.read(CollectorClusterWorker.JPATH_API_RESULT);
			//resultList = (List)SerializationUtils.clone((ArrayList)resultList);
		} else {
			log.error("에러: code가 10001이 아닙니다.");
		}
		
		return resultList;
	}
	
	/**
	 * Cluster의 상세정보(노드가 포함된 정보) 분석
	 * @param _ctx
	 * @return
	 */
	private List<Map<String, Object>> annlyApiResultFromClusterDetail(DocumentContext _ctx) {
		List<Map<String, Object>> resultList = null;

		// code 값 확인
		String code = _ctx.read(CollectorClusterWorker.JPATH_CODE);
		if (APIConstant.API_RESULT_CODE_OK.equals(code)) {
			if(_ctx.read(CollectorClusterWorker.JPATH_API_RESULT) == null)
				return null;
			
			resultList = _ctx.read(CollectorClusterWorker.JPATH_API_RESULT_NODES);
			//resultList = (List)SerializationUtils.clone((ArrayList)resultList);
		} else {
			log.error("에러: code가 10001이 아닙니다.");
		}

		return resultList;
	}
	
	/**
	 * 리스트 정보와 상세정보의 데이터를 합하기위해 보완하는 함수
	 * @param _ctx
	 * @param _cluserMap
	 */
	private void supplementCluster(DocumentContext _ctx, Map<String, Object> _cluserMap) {
		Map<String, Object> allMap = _ctx.read(CollectorClusterWorker.JPATH_API_RESULT);
		
		if(allMap == null)
			return;
		
		//allMap은 상세정보에서 추출한 값이고, clusterMap은 리스트에서 있는 값으로, 리스트에는 없으나, 상세에 있는 키,값을 리스트쪽에 넣어주기위함.
		//그래서 정보와 상세정보를 동일하게 유지하도록 함, 각 값은 동일한 시점에 추출한 것으로 여기서 키값만 비교하고, 값은 비교하지 않는다.
		for (Map.Entry<String, Object> entry : allMap.entrySet()) {
			if(!entry.getKey().equals("nodes")) {
				_cluserMap.putIfAbsent(entry.getKey(), entry.getValue());
			}
        }
	}
	

	/**
	 * DB에 입력하고 Queue에 등록
	 * @param _cl_uid
	 * @param _clusterMap
	 * @return
	 */
	private Cluster insertCluster(Integer _cl_uid, Map<String, Object> _clusterMap) {
		Cluster cluster = new Cluster();
		cluster.setUid(_cl_uid);
		cluster.setNm((String)_clusterMap.get(StringConstant.STR_name));
		cluster.setMemo((String)_clusterMap.get(StringConstant.STR_description));
		
		//http://x.x.x.x/graph가 있으면 graph를 없애고 처리한다.
		String sTmp = (String)_clusterMap.get(StringConstant.STR_prometheusUrl);
		if(sTmp != null) {
			int nIndex = sTmp.indexOf(this.url_graph);
			if(nIndex != -1) {
				sTmp = sTmp.substring(0, nIndex);
			}
			cluster.setPromUrl(sTmp);
		}
		
		sTmp = (String)_clusterMap.get(StringConstant.STR_status);
		cluster.setStatusString(sTmp);
		if(StringConstant.STR_finished.equalsIgnoreCase(sTmp) || StringConstant.STR_healthy.equalsIgnoreCase(sTmp)) {
			cluster.setDeleteAt(StringConstant.STR_N);
			cluster.setStatus(Boolean.TRUE);
		}else {
			cluster.setDeleteAt(StringConstant.STR_Y);
			cluster.setStatus(Boolean.FALSE);
		}
		
		sTmp = (String)_clusterMap.get(StringConstant.STR_version);
		cluster.setKubeVer(sTmp);
		

		try {
			sTmp = (String)_clusterMap.get(StringConstant.STR_createAt);
			cluster.setCreateAt(StringUtil.parseFlexibleLocalDateTime(sTmp));
		}catch(IllegalArgumentException e) {	}
		
		String sJson = JSONUtil.getJsonStringFromMap(_clusterMap);  //json 원문
		String sHashVal = HashUtil.getMD5Hash(sJson);               //json 원문 요약값(Hash값)
		
		cluster.setInfo(sJson);
		cluster.setHashVal(sHashVal);
		
		try {
			Cluster oldObj = this.apiQ.getApiClusterMap().get(cluster.getUniqueKey());
			
			if(oldObj != null) {
				if(!oldObj.getHashVal().equals(cluster.getHashVal()))
					this.service.updateClusterAndInsertHistory(cluster);
			}else {				
				oldObj = null;
				if(this.isFirst) {
					oldObj = this.service.selectCluster(cluster);
				}
				if(oldObj == null)
					this.service.insertCluster(cluster);
			}
			
		} catch (Exception e) {
			log.error("데이터 Insert 에러 Cluster:\n{}", cluster, e);
		}finally {
			cluster.setSessionId(SESSIONID);
			this.apiQ.putApiMap(cluster);  //key=> cluster.getUniqueKey();
		}
		
		return cluster;
	}
	
	private void insertClusterNode(Cluster _cl, Map<String, Object> _nodeMap) {
		ClusterNode node = new ClusterNode();
		String sTmp = null;		
		String sUuid       = (String)_nodeMap.get(StringConstant.STR_uid);
		String sStatus     = (String)_nodeMap.get(StringConstant.STR_status);
		
		node.setNoUuid(sUuid);
		node.setClUid(_cl.getUid());
		
		if(StringConstant.STR_true.equals(sStatus)) {
			node.setDeleteAt(StringConstant.STR_N);
		}else {
			node.setDeleteAt(StringConstant.STR_Y);
		}
		node.setNm((String)_nodeMap.get(StringConstant.STR_name));
		node.setMemo((String)_nodeMap.get(StringConstant.STR_description));
		node.setStatus((String)_nodeMap.get(StringConstant.STR_status));
		
		Object obj = _nodeMap.get("usageDto");
		if(obj instanceof Map) {
			node.setUsageDto(JSONUtil.convertMapToObject((Map)obj, ClusterNode.UsageDto.class));
		}
		
		try {
			sTmp = (String)_nodeMap.get(StringConstant.STR_createdAt);
			node.setCreatedAt(StringUtil.parseFlexibleLocalDateTime(sTmp));
		}catch(IllegalArgumentException e) {	}
		
		obj = _nodeMap.get(StringConstant.STR_role);
		if(obj instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray)obj;
			sTmp= jsonArray.stream()
                .map(Object::toString)
                .collect(Collectors.joining(StringConstant.STR_COMMA));
			node.setRole(sTmp);
		}
		
		//GPU driver관련
		node.setGpuDriverString((String)_nodeMap.get(StringConstant.STR_driverVersion));
		node.setCudaString((String)_nodeMap.get(StringConstant.STR_cudaVersion));
		
		//labels값을 모두 등록함
		HashMap<String, String> map = (HashMap)_nodeMap.get(StringConstant.STR_labels);
		Map<String, String> labels = node.getLabels();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			labels.putIfAbsent(entry.getKey(), entry.getValue());
        }
		
		//{{프로메테우스 metric 수집중 노드의 GPU를 가져올려고
		String gpuJsonString = null;
		PromMetricNodes mNodes = (PromMetricNodes)this.promQ.getPromDequesFirstObject(PromDequeName.METRIC_NODEINFO);
		if(mNodes != null) {
			PromMetricNode mNode = mNodes.getMetricNode(_cl.getUid(), node.getNm());
			node.setPromMetricNode(mNode);
			
			Map<Integer, PromMetricNodeGPU> gpulist = mNode.getMGpuList();
			
			try {
				gpuJsonString = JSONUtil.getJsonstringFromObject(gpulist,this.gpuFilterSet);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			node.setGpuinfo(gpuJsonString);			
		}
		//}}GPU 리스트
		
		String sJson = JSONUtil.getJsonStringFromMap(_nodeMap);
		String sHashVal = HashUtil.getMD5Hash(sJson + gpuJsonString);
		
		node.setInfo(sJson);
		node.setHashVal(sHashVal);
		
		try {
			ClusterNode oldObj = this.apiQ.getApiClusterNodeMap().get(node.getUniqueKey());
			if(oldObj != null) {
				node.setUid(oldObj.getUid());
				if(!oldObj.getHashVal().equals(node.getHashVal()))
					this.service.updateClusterNodeAndInsertHistory(node);
			}else {			
				oldObj = null;
				if(this.isFirst) {
					oldObj = this.service.selectClusterNode(node); // 키값이 DB에서 생성되었기에 unique 인덱스로 쿼리를 진행함
					node.setUid(oldObj.getUid());
				}
				if(oldObj == null)
					this.service.insertClusterNode(node);
			}
		} catch (Exception e) {
			log.error("데이터 Insert 에러 ClusterNode:\n{}", node, e);
		}finally {
			node.setSessionId(SESSIONID);
			this.apiQ.putApiMap(node); //key : getUniqueKey()
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
		
		return json_string;
	}	
}

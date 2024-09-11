package com.kware.policy.task.collector.controller;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kware.common.config.serializer.HumanReadableSizeSerializer;
import com.kware.common.config.serializer.JsonIgnoreDynamicSerializer;
import com.kware.common.openapi.vo.APIPagedResponse;
import com.kware.common.openapi.vo.APIResponseCode;
import com.kware.common.util.JSONUtil;
import com.kware.policy.task.collector.service.ResourceUsageService;
import com.kware.policy.task.collector.service.vo.Cluster;
import com.kware.policy.task.collector.service.vo.ClusterNode;
import com.kware.policy.task.collector.service.vo.ClusterWorkload;
import com.kware.policy.task.collector.service.vo.ClusterWorkloadPod;
import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.collector.service.vo.PromMetricNodes;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.collector.service.vo.PromMetricPods;
import com.kware.policy.task.collector.service.vo.ResourceUsageNode;
import com.kware.policy.task.collector.service.vo.ResourceUsagePod;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;

/**
 * 외부에 서비스가 필요할때 사용할 용도 
 */
@SuppressWarnings("rawtypes")
@RestController
@RequestMapping("/queue")
public class CollectorRestController {
	static Logger logger = LoggerFactory.getLogger(CollectorRestController.class);
	QueueManager qm = QueueManager.getInstance();
	APIQueue     apiQ = null;
	PromQueue    promQ = null;
	RequestQueue requestQ = null;
	
	@Autowired
	ResourceUsageService ruService;
	
	@PostConstruct
	private void init() {
		apiQ     = qm.getApiQ();
		promQ    = qm.getPromQ();
		requestQ = qm.getRequestQ();
		
		HumanReadableSizeSerializer.setHumanReadable(false);
	}
	
	/**
	 * 개발할때만 편의상 잘 보도록 하기 위함
	 * @param config
	 * @return
	 */
	@GetMapping("/config/set/hummanreadable/{val}")
    public ResponseEntity<Boolean> setHummanReadable(@PathVariable("val") Integer val) {
		if(val == 1) {
			HumanReadableSizeSerializer.setHumanReadable(true);
			return ResponseEntity.ok(true);
		}else {
			HumanReadableSizeSerializer.setHumanReadable(false);
			return ResponseEntity.ok(false);
		}
    }
	
	/**
	 * 개발할때만 편의상 잘 보도록 하기 위함
	 * @param config
	 * @return
	 */
	@GetMapping("/config/set/jsonignore/{val}")
    public ResponseEntity<Boolean> setIgnorDynamic(@PathVariable("val") Integer val) {
		if(val == 1) {
			JsonIgnoreDynamicSerializer.setIgnoreDynamic(true);
			return ResponseEntity.ok(true);
		}else {
			JsonIgnoreDynamicSerializer.setIgnoreDynamic(false);
			return ResponseEntity.ok(false);
		}
    }
	
	
	@GetMapping("/api/clusters")
    public ResponseEntity<Map> getCluster() {
    	return ResponseEntity.ok(apiQ.getApiClusterMap());
    }
	
	@GetMapping("/api/cluster/{id}")
    public ResponseEntity<Cluster> getClusterId(@PathVariable String id) {
    	return ResponseEntity.ok(apiQ.getApiClusterMap().get(id));
    }
	
	@GetMapping("/api/cluster/nodes")
    public ResponseEntity<Map> getClusterNodes() {
    	return ResponseEntity.ok(new TreeMap<>(apiQ.getApiClusterNodeMap()));
    }
	
	@GetMapping("/api/cluster/node/{id}")
    public ResponseEntity<ClusterNode> getClusterNodeId(@PathVariable String id) {
    	return ResponseEntity.ok(apiQ.getApiClusterNodeMap().get(id));
    }
	
	@GetMapping("/api/workloads")
    public ResponseEntity<Map> getWorkloads() {
    	return ResponseEntity.ok(apiQ.getApiWorkloadMap());
    }
	
	@GetMapping("/api/workload/{id}")
    public ResponseEntity<ClusterWorkload> getWorkloadId(@PathVariable String id) {
    	return ResponseEntity.ok(apiQ.getApiWorkloadMap().get(id));
    }
	
	@GetMapping("/api/workload/{id}/pods")
    public ResponseEntity<Map> getWorkloadIdPods(@PathVariable String id) {
    	return ResponseEntity.ok(apiQ.getApiWorkloadMap().get(id).getResourceMap());
    }
	
	@GetMapping("/api/workload/pods")
    public ResponseEntity<Map> getWorkloadPods() {
    	return ResponseEntity.ok(apiQ.getApiWorkloadPodMap());
    }
	
	@GetMapping("/api/workload/pod/{id}")
    public ResponseEntity<ClusterWorkloadPod> getWorkloadPodId(@PathVariable String id) {
    	return ResponseEntity.ok(apiQ.getApiWorkloadPodMap().get(id));
    }
		
	@GetMapping("/metric/nodes")
    public ResponseEntity<Map> getMetricdNodes() {
		PromMetricNodes pmns = promQ.getPromNodesDeque().peekFirst();
    	return ResponseEntity.ok(pmns.getNodesMap());
    }

	@GetMapping("/metric/node/{clId}/{id}")
    public ResponseEntity<PromMetricNode> getMetricdNodeId(@PathVariable Integer clId, @PathVariable String id) {
		PromMetricNodes pmns = promQ.getPromNodesDeque().peekFirst();
		return ResponseEntity.ok(pmns.getMetricNode(clId, id));
    }
	
	@GetMapping("/metric/node/{id}")
    public ResponseEntity<PromMetricNode> getMetricdNodeId(@PathVariable String id) {
		PromMetricNodes pmns = promQ.getPromNodesDeque().peekFirst();
		return ResponseEntity.ok(pmns.getMetricNode(id));
    }
	
	@GetMapping("/metric/pods")
    public ResponseEntity<Map> getMetricdPods() {
		PromMetricPods pmps = promQ.getPromPodsDeque().peekFirst();
		return ResponseEntity.ok(pmps.getPodsMap());
    }
    
	@GetMapping("/metric/pod/{clId}/{id}")
    public ResponseEntity<PromMetricPod> getMetricdPodId(@PathVariable Integer clId, @PathVariable String id) {
		PromMetricPods pmps = promQ.getPromPodsDeque().peekFirst();
		return ResponseEntity.ok(pmps.getMetricPod(clId, id));
    }
	
	@GetMapping("/metric/pod/{id}")
    public ResponseEntity<PromMetricPod> getMetricdPodId(@PathVariable String id) {
		PromMetricPods pmps = promQ.getPromPodsDeque().peekFirst();
		return ResponseEntity.ok(pmps.getMetricPod(id));
    }
	
	/**
	 * DB상의 사용량을 가져오는 쿼리
	 * requestbody에 값이 없으면 최근 30분의 데이터를 조회
	 * @param apReq
	 * @return
	 */
	
	@SuppressWarnings("unchecked")
	@GetMapping("/db/usage/nodes")
    public ResponseEntity<APIPagedResponse> getDBUsageNodes(@RequestBody(required = false) ResourceUsageNode apReq) {
		if(apReq == null) {
			apReq = new ResourceUsageNode();
			apReq.setDefautPage10();
		}
		List list = ruService.selectResourceUsageNodeList(apReq);
		list = convertToMapFromJsonstring(list);
		
		APIResponseCode arCode = APIResponseCode.SUCCESS; 
		APIPagedResponse apResponse = new APIPagedResponse<Map>(arCode.getCode(), arCode.getMessage(), list, apReq.getPageNumber(), apReq.getPageSize());
		return ResponseEntity.ok(apResponse);
		//return ResponseEntity.ok(list);
    }
	
	@SuppressWarnings("unchecked")
	@GetMapping("/db/usage/nodes/{pageNumber}")
    public ResponseEntity<APIPagedResponse> getDBUsageNodesPages(@RequestBody(required = false) ResourceUsageNode apReq, @PathVariable int pageNumber) {
		if(apReq == null) {
			apReq = new ResourceUsageNode();
			apReq.setDefautPage10();
			apReq.setPageNumber(pageNumber);
		}
		List list = ruService.selectResourceUsageNodeList(apReq);
		list = convertToMapFromJsonstring(list);
		
		APIResponseCode arCode = APIResponseCode.SUCCESS; 
		APIPagedResponse apResponse = new APIPagedResponse<Map>(arCode.getCode(), arCode.getMessage(), list, apReq.getPageNumber(), apReq.getPageSize());
		return ResponseEntity.ok(apResponse);
		//return ResponseEntity.ok(list);
    }
	
	@SuppressWarnings("unchecked")
	@GetMapping("/db/usage/pods")
    public ResponseEntity<APIPagedResponse> getDBUsagePods(@RequestBody(required = false) ResourceUsagePod apReq) {
		if(apReq == null) {
			apReq = new ResourceUsagePod();
			apReq.setDefautPage10();
		}
		List list = ruService.selectResourceUsagePodList(apReq);
		list = convertToMapFromJsonstring(list);
		
		APIResponseCode arCode = APIResponseCode.SUCCESS; 
		APIPagedResponse apResponse = new APIPagedResponse<Map>(arCode.getCode(), arCode.getMessage(), list, apReq.getPageNumber(), apReq.getPageSize());
		return ResponseEntity.ok(apResponse);
		//return ResponseEntity.ok(list);
    }
	
	@SuppressWarnings("unchecked")
	@GetMapping("/db/usage/pods/{pageNumber}")
    public ResponseEntity<APIPagedResponse> getDBUsagePodsPages(@RequestBody(required = false) ResourceUsagePod apReq, @PathVariable int pageNumber) {
		if(apReq == null) {
			apReq = new ResourceUsagePod();
			apReq.setDefautPage10();
			apReq.setPageNumber(pageNumber);
		}
		List list = ruService.selectResourceUsagePodList(apReq);
		list = convertToMapFromJsonstring(list);
		
		APIResponseCode arCode = APIResponseCode.SUCCESS; 
		APIPagedResponse apResponse = new APIPagedResponse<Map>(arCode.getCode(), arCode.getMessage(), list, apReq.getPageNumber(), apReq.getPageSize());
		return ResponseEntity.ok(apResponse);
		//return ResponseEntity.ok(list);
    }
	
	private List convertToMapFromJsonstring(List<Map<String, Object>> list){
		for (Map<String, Object> map : list) {
            if (map.containsKey("results") && map.get("results") instanceof String) {
                String resultsJson = (String) map.get("results");
                try {
                    // JSON 문자열을 Map으로 변환하여 다시 results 필드에 저장
                	Map<String, Object> resultsMap = JSONUtil.getMapFromJsonString(resultsJson);
                    map.put("results", resultsMap);
                } catch (Exception e) {
                    e.printStackTrace();
                    // 변환 중 에러가 발생한 경우, 필요에 따라 로깅 또는 예외 처리를 할 수 있음
                }
            }
        }
		return list;
	}
    
}

package com.kware.policy.task.collector.controller;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kware.common.openapi.vo.APIPagedResponse;
import com.kware.common.openapi.vo.APIResponseCode;
import com.kware.common.util.JSONUtil;
import com.kware.policy.task.collector.service.ResourceUsageService;
import com.kware.policy.task.collector.service.vo.ResourceUsageNode;
import com.kware.policy.task.collector.service.vo.ResourceUsagePod;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 외부에 서비스가 필요할때 사용할 용도 
 */
@SuppressWarnings("rawtypes")
@Slf4j
@RestController
@RequestMapping("/queue/db")
@Tag(name = "Collector Database Usage Controller", description = "DB에 저장된 사용 정보를 제공")
public class CollectorRestQueueDBController {
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
	}	
	
	/**
	 * DB상의 사용량을 가져오는 쿼리
	 * requestbody에 값이 없으면 최근 30분의 데이터를 조회
	 * @param apReq
	 * @return
	 */
	
	@SuppressWarnings("unchecked")
	@GetMapping("/usage/nodes")
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
	@GetMapping("/usage/nodes/{pageNumber}")
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
	@GetMapping("/usage/pods")
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
	@GetMapping("/usage/pods/{pageNumber}")
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

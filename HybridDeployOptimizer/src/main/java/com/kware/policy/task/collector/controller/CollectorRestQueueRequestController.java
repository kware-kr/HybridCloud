package com.kware.policy.task.collector.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kware.common.config.serializer.HumanReadableSizeSerializer;
import com.kware.common.config.serializer.JsonIgnoreDynamicSerializer;
import com.kware.policy.task.collector.service.ResourceUsageService;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Container;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 외부에 서비스가 필요할때 사용할 용도 
 */
@SuppressWarnings("rawtypes")
@Slf4j
@RestController
@RequestMapping("/queue")
@Tag(name = "Collector Queue Request Controller", description = "큐에 보관된 배포요청 정보를 제공")
public class CollectorRestQueueRequestController {
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
	
/////////////////////////////// rquestQ //////////////////////////////////////////////////
	
	@GetMapping("/request/workloads")
	public ResponseEntity<Map> getRequestMap() {
		return ResponseEntity.ok(requestQ.getWorkloadRequestReadOlnyMap());
	}
	
	@GetMapping("/request/workload/{id}")
	public ResponseEntity<WorkloadRequest> getRequestMap(@PathVariable("id") String id) {
		WorkloadRequest wlRequest = requestQ.getWorkloadRequest(id);
		return ResponseEntity.ok(wlRequest);
	}

	@GetMapping("/request/workload/containers")
	public ResponseEntity<List> getNoqAppliedMap() {
		Map<String, WorkloadRequest> map = requestQ.getWorkloadRequestReadOlnyMap();
		List<Container> list = new ArrayList<Container>();
		map.forEach((key, val) -> {
			list.addAll(val.getRequest().getContainers());
		});
		
		return ResponseEntity.ok(list);
	}
	
	@GetMapping("/request/workload/container/{clId}/{node}")
	public ResponseEntity<List> getNoqAppliedMap(@PathVariable Integer clId, @PathVariable String node) {
		//String key = clId + StringConstant.STR_UNDERBAR + node;
		//Map map = requestQ.getWorkloadRequestNotApplyMap().get(key);
		
		Map<String, WorkloadRequest> map = requestQ.getWorkloadRequestReadOlnyMap();
		List<Container> list = new ArrayList<Container>();
		map.forEach((key, val) -> {
			if(val.getRequest().getClUid().equals(clId)) {
				for(Container container : val.getRequest().getContainers()) {
					if(node.endsWith(container.getNodeName())) {
						list.add(container);
					}
				}
			}
		});
		
		return ResponseEntity.ok(list);
	}

}

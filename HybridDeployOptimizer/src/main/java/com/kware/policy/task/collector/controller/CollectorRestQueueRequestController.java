package com.kware.policy.task.collector.controller;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

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
import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;

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
		return ResponseEntity.ok(requestQ.getWorkloadRequestMap());
	}
	
	@GetMapping("/request/workload/{id}")
	public ResponseEntity<WorkloadRequest> getRequestMap(@PathVariable("id") String id) {
		WorkloadRequest wlRequest = requestQ.getWorkloadRequestMap().get(id);
		return ResponseEntity.ok(wlRequest);
	}

	@GetMapping("/request/workload/containers")
	public ResponseEntity<Map> getNoqAppliedMap() {
		return ResponseEntity.ok(requestQ.getWorkloadRequestNotApplyMap());
	}
	
	@GetMapping("/request/workload/container/{clId}/{node}")
	public ResponseEntity<Map> getNoqAppliedMap(@PathVariable Integer clId, @PathVariable String node) {
		String key = clId + StringConstant.STR_UNDERBAR + node;
		Map map = requestQ.getWorkloadRequestNotApplyMap().get(key);
		return ResponseEntity.ok(map);
	}

}

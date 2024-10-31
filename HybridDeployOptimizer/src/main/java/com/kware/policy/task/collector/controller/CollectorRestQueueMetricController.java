package com.kware.policy.task.collector.controller;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kware.policy.task.collector.service.ResourceUsageService;
import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.collector.service.vo.PromMetricNodes;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.collector.service.vo.PromMetricPods;
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
@RequestMapping("/queue/metric")
@Tag(name = "Collector Queue Prometheus metric Controller", description = "통합 프로메테우스로부터 수집된 정보를 제공")
public class CollectorRestQueueMetricController {
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
	

/////////////////////////////// promQ //////////////////////////////////////////////////
	
	@GetMapping("/nodes")
    public ResponseEntity<Map> getMetricdNodes() {
		PromMetricNodes pmns = promQ.getPromNodesDeque().peekFirst();
    	return ResponseEntity.ok(pmns.getNodesMap());
    }

	@GetMapping("/node/{clId}/{id}")
    public ResponseEntity<PromMetricNode> getMetricdNodeId(@PathVariable Integer clId, @PathVariable String id) {
		PromMetricNodes pmns = promQ.getPromNodesDeque().peekFirst();
		return ResponseEntity.ok(pmns.getMetricNode(clId, id));
    }
	
	@GetMapping("/node/{id}")
    public ResponseEntity<PromMetricNode> getMetricdNodeId(@PathVariable String id) {
		PromMetricNodes pmns = promQ.getPromNodesDeque().peekFirst();
		return ResponseEntity.ok(pmns.getMetricNode(id));
    }
	
	@GetMapping("/pods")
    public ResponseEntity<Map> getMetricdPods() {
		PromMetricPods pmps = promQ.getPromPodsDeque().peekFirst();
		return ResponseEntity.ok(pmps.getPodsMap());
    }
    
	@GetMapping("/pod/{clId}/{id}")
    public ResponseEntity<PromMetricPod> getMetricdPodId(@PathVariable Integer clId, @PathVariable String id) {
		PromMetricPods pmps = promQ.getPromPodsDeque().peekFirst();
		return ResponseEntity.ok(pmps.getMetricPod(clId, id));
    }
	
	@GetMapping("/pod/{id}")
    public ResponseEntity<PromMetricPod> getMetricdPodId(@PathVariable String id) {
		PromMetricPods pmps = promQ.getPromPodsDeque().peekFirst();
		return ResponseEntity.ok(pmps.getMetricPod(id));
    }    
}

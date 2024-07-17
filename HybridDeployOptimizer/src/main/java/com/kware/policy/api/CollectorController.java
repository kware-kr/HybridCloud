package com.kware.policy.api;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kware.policy.common.QueueManager;

/**
 * 외부에 서비스가 필요할때 사용할 용도 
 */
@RestController
@RequestMapping("/queue")
public class CollectorController {
	static Logger logger = LoggerFactory.getLogger(CollectorController.class);

	
	
	@GetMapping("/api/clusters")
    public ResponseEntity<Map> getCluster() {
    	QueueManager qm = QueueManager.getInstance();
    	return ResponseEntity.ok(qm.getApiLastClusters());
    }
	
	@GetMapping("/api/cluster/nodes")
    public ResponseEntity<Map> getClusterNodes() {
    	QueueManager qm = QueueManager.getInstance();
    	return ResponseEntity.ok(qm.getApiLastClusterNodes());
    }
	
	@GetMapping("/api/workloads")
    public ResponseEntity<Map> getWorkloads() {
    	QueueManager qm = QueueManager.getInstance();
    	return ResponseEntity.ok(qm.getApiLastWorkloads());
    }
	
	@GetMapping("/api/workload/pods")
    public ResponseEntity<Map> getWorkloadPods() {
    	QueueManager qm = QueueManager.getInstance();
    	return ResponseEntity.ok(qm.getApiLastWorkloadPods());
    }
		
	@GetMapping("/metric/nodes")
    public ResponseEntity<List> getMetricdNodes() {
    	QueueManager qm = QueueManager.getInstance();
    	return ResponseEntity.ok(qm.getLastPromMetricNodesReadOnly());
    }
	
	@GetMapping("/metric/pods")
    public ResponseEntity<List> getMetricdPods() {
    	QueueManager qm = QueueManager.getInstance();
    	return ResponseEntity.ok(qm.getLastPromMetricPodsReadOnly());
    }
}

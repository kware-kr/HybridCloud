package com.kware.policy.task.collector.controller;

import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kware.policy.task.collector.service.ResourceUsageService;
import com.kware.policy.task.collector.service.vo.Cluster;
import com.kware.policy.task.collector.service.vo.ClusterNode;
import com.kware.policy.task.collector.service.vo.ClusterWorkload;
import com.kware.policy.task.collector.service.vo.ClusterWorkloadPod;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.APIQueue.APIMapsName;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 외부에 서비스가 필요할때 사용할 용도 
 */
@SuppressWarnings("rawtypes")
@Slf4j
@RestController
@RequestMapping("/queue/api")
@Tag(name = "Collectort Queue API Controller", description = "스트라토 api로부터 수집된 정보를 제공")
public class CollectorRestQueueAPIController {
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
	
	
///////////////////////////////////////// apiQ /////////////////////////////////////////	
	@Operation(summary = "Get All Cluster", description = "Retrieve All Cluster Lists")
	@GetMapping("/clusters")
    public ResponseEntity<Map> getCluster() {
    	return ResponseEntity.ok(apiQ.getReadOnlyApiMap(APIMapsName.CLUSTER));
    }
	
	@GetMapping("/cluster/{id}")
    public ResponseEntity<Cluster> getClusterId(@PathVariable String id) {
    	return ResponseEntity.ok((Cluster)apiQ.getReadOnlyApiMap(APIMapsName.CLUSTER).get(id));
    }
	
	@GetMapping("/cluster/nodes")
    public ResponseEntity<Map> getClusterNodes() {
    	return ResponseEntity.ok(new TreeMap<>(apiQ.getReadOnlyApiMap(APIMapsName.NODE)));
    }
	
	@GetMapping("/cluster/node/{id}")
    public ResponseEntity<ClusterNode> getClusterNodeId(@PathVariable String id) {
    	return ResponseEntity.ok((ClusterNode)apiQ.getReadOnlyApiMap(APIMapsName.NODE).get(id));
    }
	
	@GetMapping("/workloads")
    public ResponseEntity<Map> getWorkloads() {
    	return ResponseEntity.ok(apiQ.getReadOnlyApiMap(APIMapsName.WORKLOAD));
    }
	
	@GetMapping("/workload/{id}")
    public ResponseEntity<ClusterWorkload> getWorkloadId(@PathVariable String id) {
    	return ResponseEntity.ok((ClusterWorkload)apiQ.getReadOnlyApiMap(APIMapsName.WORKLOAD).get(id));
    }
	
	@GetMapping("/workload/{id}/pods")
    public ResponseEntity<Map> getWorkloadIdPods(@PathVariable String id) {
    	return ResponseEntity.ok(((ClusterWorkload)apiQ.getReadOnlyApiMap(APIMapsName.WORKLOAD).get(id)).getResourceMap());
    }
	
	@GetMapping("/workload/pods")
    public ResponseEntity<Map> getWorkloadPods() {
    	return ResponseEntity.ok(apiQ.getReadOnlyApiMap(APIMapsName.WORKLOADPOD));
    }
	
	@GetMapping("/workload/pod/{id}")
    public ResponseEntity<ClusterWorkloadPod> getWorkloadPodId(@PathVariable String id) {
    	return ResponseEntity.ok((ClusterWorkloadPod)apiQ.getReadOnlyApiMap(APIMapsName.WORKLOADPOD).get(id));
    }
}

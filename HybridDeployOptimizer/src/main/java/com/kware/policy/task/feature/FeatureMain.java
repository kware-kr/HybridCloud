package com.kware.policy.task.feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kware.policy.task.feature.service.FeatureService;
import com.kware.policy.task.feature.service.vo.ClusterFeature;
import com.kware.policy.task.feature.service.vo.ClusterNodeFeature;
import com.kware.policy.task.feature.service.vo.MinResourceCapacity;
import com.kware.policy.task.feature.service.vo.NodeScalingPolicy;
import com.kware.policy.task.feature.service.vo.PodScalingPolicy;
import com.kware.policy.task.feature.service.vo.WorkloadFeature;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 */

@Slf4j
@Component
public class FeatureMain {

	// @Autowired
	public FeatureMain() {
	}

	@Autowired
	private FeatureService ftService;

	/*
	@Autowired
	private PromQLService pqService;

	@Autowired
	private ClusterManagerService cmService;

	@Autowired
	private ResourceUsageService ruService;

	final QueueManager qm = QueueManager.getInstance();

	APIQueue apiQ = qm.getApiQ();
	PromQueue promQ = qm.getPromQ();
*/

	private Map<String, Object> featureMap = null;
	private Map<String, ClusterNodeFeature> nodeFeatureMap = new HashMap<String, ClusterNodeFeature>();
	private Map<Integer, ClusterFeature> clusterFeatureMap = new HashMap<Integer, ClusterFeature>();

	@PostConstruct
	private void init() {
		init_workload_feature();
		init_cluster_feature();
		init_node_feature();
	}
	
	final static String workload_feature      = "workload_feature";
	final static String	node_scaling_policies = "node_scaling_policies";
	final static String	pod_scaling_policies  = "pod_scaling_policies";
	final static String	min_resource_capacity = "min_resource_capacity";

	/**
	 * 특성 기준정보를 가져온다.
	 */
	public Map<String, Object> getFeatureBase() {
		return this.featureMap;
	}
	
	public List<WorkloadFeature> getFeatureBase_workloadFeature() {
		if(this.featureMap == null) {
			init();
		}
		
		Object obj = this.featureMap.get(workload_feature);
		if(obj != null) {
			return (List<WorkloadFeature>)obj;
		}
		return null;
	}
	
	public NodeScalingPolicy getFeatureBase_nodeScalingPolicies() {
		if(this.featureMap == null) {
			init();
		}
		
		Object obj = this.featureMap.get(node_scaling_policies);
		if(obj != null) {
			return (NodeScalingPolicy)obj;
		}
		return null;
	}
	
	public PodScalingPolicy getFeatureBase_podScalingPolicies() {
		if(this.featureMap == null) {
			init();
		}
		Object obj = this.featureMap.get(pod_scaling_policies);
		if(obj != null) {
			return (PodScalingPolicy)obj;
		}
		return null;
	}
	
	public MinResourceCapacity getFeatureBase_minResourceCapacity() {
		Object obj = this.featureMap.get(min_resource_capacity);
		if(obj != null) {
			return (MinResourceCapacity)obj;
		}
		return null;
	}
	////////////////////////////////////////////////////////////////////////////
	
	public Map<String, ClusterNodeFeature> getClusterNodeFeature(){
		return this.nodeFeatureMap;
	}
	
	public ClusterNodeFeature getClusterNodeFeature_nodeKey(String _nodeKey){
		return this.nodeFeatureMap.get(_nodeKey);
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	public Map<Integer, ClusterFeature> getClusterFeature(){
		return this.clusterFeatureMap;
	}
	
	public ClusterFeature getClusterFeature_cluid(Integer _clUid){
		return this.clusterFeatureMap.get(_clUid);
	}
	
	
	//프론트에서 변경되었을때 다시 읽어들이는 역할을 한다.
	/////////////////////////////////////////////////////////////////////////////

	public void init_workload_feature() {
		try {
			Map<String, Object>  oldmap = this.featureMap;
			this.featureMap = ftService.getCommonFeatuerListALL();
			if(oldmap != null)
				oldmap.clear();
			oldmap = null;
		} catch (Exception e) {

			log.error("init_node_feature Error",e);
		}
	}
	public void init_node_feature() {
		try {
			Map<String, ClusterNodeFeature> oldmap = this.nodeFeatureMap;
			this.nodeFeatureMap    = ftService.getClusterNodeFeatureListAll();
			if(oldmap != null)
				oldmap.clear();
			oldmap = null;
		} catch (Exception e) {
			log.error("init_node_feature Error",e);
		}
	}
	
	public void init_cluster_feature() {
		try {
			Map<Integer, ClusterFeature> oldmap = this.clusterFeatureMap;
			this.clusterFeatureMap = ftService.getClusterFeatureListAll();
			if(oldmap != null)
				oldmap.clear();
			oldmap = null;
		} catch (Exception e) {
			log.error("init_node_feature Error",e);
		}
	}
	
	public void setClusterNodeFeature(List<ClusterNodeFeature>  cnfs ) {
		for(ClusterNodeFeature cnf: cnfs) {		
			ftService.updateClusterNodeAutoFeature(cnf);
		}
	}


}

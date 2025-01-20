package com.kware.policy.task.feature.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kware.common.util.JSONUtil;
import com.kware.policy.task.feature.service.dao.FeatureDao;
import com.kware.policy.task.feature.service.vo.ClusterFeature;
import com.kware.policy.task.feature.service.vo.ClusterNodeFeature;
import com.kware.policy.task.feature.service.vo.MinResourceCapacity;
import com.kware.policy.task.feature.service.vo.NodeScalingPolicy;
import com.kware.policy.task.feature.service.vo.PodScalingPolicy;
import com.kware.policy.task.feature.service.vo.WorkloadFeature;

@Service
public class FeatureService {

	@Autowired
	protected FeatureDao dao;
	
	
	/*********************** Mo_common_feature_base table 
	 * @throws Exception ********************/
	public HashMap<String,Object>  getCommonFeatuerListALL() throws Exception {
		List<HashMap<String,Object>> list =  dao.selectCommonFeatuerListALL();
		
		HashMap<String,Object> rsMap = new HashMap<String,Object>();
		
		String key = "fea_name";
		String saveKey = "fea_content";
		String key_val = null;
		String temp = null;
		for(HashMap<String,Object> m : list) {
			String key_vla = (String)m.get(key);
			if(key_vla.equals("min_resource_capacity")) {
				temp = (String)m.get(saveKey);
				MinResourceCapacity f = JSONUtil.fromJson(temp, MinResourceCapacity.class);
				rsMap.put(key_vla, f);
			}else if(key_vla.equals("node_scaling_policies")) {
				temp = (String)m.get(saveKey);
				NodeScalingPolicy f = JSONUtil.fromJson(temp, NodeScalingPolicy.class);
				rsMap.put(key_vla, f);
			}else if(key_vla.equals("pod_scaling_policies")) {
				temp = (String)m.get(saveKey);
				PodScalingPolicy f = JSONUtil.fromJson(temp, PodScalingPolicy.class);
				rsMap.put(key_vla, f);
			}else if(key_vla.equals("workload_feature")) {
				temp = (String)m.get(saveKey);
				WorkloadFeature f = JSONUtil.fromJson(temp, WorkloadFeature.class);
				
				List<WorkloadFeature> workload_list = null;
				Object obj = rsMap.get(key_vla);
				if(obj == null) {
					workload_list = new ArrayList<WorkloadFeature>();
				}else {
					workload_list = (List<WorkloadFeature>)obj;
				}
				workload_list.add(f);
				rsMap.put(key_vla, workload_list);
			}
			m.clear();
		}
		list.clear();
		
		Object obj  = rsMap.get("workload_feature");
		if(obj != null) {
			List workload_list = (List)obj;
			workload_list.sort(Comparator.comparingInt(WorkloadFeature::getId));
		}
		
		return rsMap;
	}
	
	
	/*********************** Mo_cluster_node_feature table 
	 * @throws Exception ********************/

	public HashMap<String,ClusterNodeFeature> getClusterNodeFeatureListAll() throws Exception {
		List<HashMap<String,Object>> list = dao.selectClusterNodeFeatureListAll();
		
		HashMap<String,ClusterNodeFeature> rsMap = new HashMap<String,ClusterNodeFeature>();
		
		String key = null;
		for(HashMap<String,Object> m : list) {
			key = "cl_uid";
			Integer clUid = (Integer)m.get(key);
			
			key = "nm";
			String nm = (String)m.get(key);
			
			ClusterNodeFeature f  = null;
			ClusterNodeFeature af = null;
			key = "feature";
			String temp = (String)m.get(key);
			if(temp != null) {
				f = JSONUtil.fromJson(temp, ClusterNodeFeature.class);
				//m.put(key, f);
				f.setClUid(clUid);
				f.setNodeName(nm);
			}
			key = "auto_feature";
			temp = (String)m.get(key);
			if(temp != null) {
				af = JSONUtil.fromJson(temp, ClusterNodeFeature.class);
				//m.put(key, af);
				af.setClUid(clUid);
				af.setNodeName(nm);
			}
			
			if(f != null) {
				rsMap.put(clUid+nm, f);
			} else if(af != null) {
				rsMap.put(clUid+nm, af);
			}
			
			m.clear();
			
			
		}
		list.clear();
		return rsMap;
	}
	
	public HashMap<Integer,ClusterFeature> getClusterFeatureListAll() throws Exception {
		List<HashMap<String,Object>> list =  dao.selectClusterFeatureListAll();
		
		HashMap<Integer,ClusterFeature> rsMap = new HashMap<Integer,ClusterFeature>();
		String key = null;
		for(HashMap<String,Object> m : list) {
			key = "feature";
			String temp = (String)m.get(key);
			if(temp != null) {
				ClusterFeature f = JSONUtil.fromJson(temp, ClusterFeature.class);
				//m.put(key, f);
				
				key = "cl_uid";
				Integer nTemp = (Integer)m.get(key);
				f.setClUid(nTemp);
				rsMap.put(nTemp, f);
				
				m.clear();
			}
		}
		list.clear();
		return rsMap;
	}
	
	public void updateClusterNodeAutoFeature(ClusterNodeFeature vo) {
		dao.updateClusterNodeAutoFeature(vo);
	}
}

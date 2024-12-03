package com.kware.policy.task.feature.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kware.policy.task.feature.service.dao.FeatureDao;
import com.kware.policy.task.feature.service.vo.ClusterNodeFeature;
import com.kware.policy.task.feature.service.vo.CommonFeatureBase;

@Service
public class FeatureService {

	@Autowired
	protected FeatureDao dao;
	

	//common_feature_base table{{
	public List<CommonFeatureBase> selectFeatureBaseListALL() {
		return (List<CommonFeatureBase>) dao.selectFeatureBaseListALL();
	}
	//}} common_feature_base table
	
	
	//cluster_node_feature talbe {{
	
	public List<ClusterNodeFeature> selectClusterNodeFeatureListAll() {
		return (List<ClusterNodeFeature>) dao.selectClusterNodeFeatureListAll();
	}
	
	public void insertClusterNodeFeature(ClusterNodeFeature vo) {
		dao.insertClusterNodeFeature(vo);
	}
	
	public void InsertClusterNodeFeatureHistory(ClusterNodeFeature vo) {
		dao.insertClusterNodeFeatureHistory(vo);
	}
	//}} cluster_node_feature talbe
}

package com.kware.policy.task.feature.service.dao;

import java.util.HashMap;
import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kware.common.util.JSONUtil;
import com.kware.policy.task.feature.service.vo.ClusterNodeFeature;

@Repository
public class FeatureDao {
	
	@Autowired
	@Qualifier("sqlSessionTemplate")
	SqlSessionTemplate sqlSessionTemplate;

	/*********************** Mo_common_feature_base table ********************/
	public List<HashMap<String,Object>> selectCommonFeatuerListALL() {
		return sqlSessionTemplate.selectList("featureMapper.selectCommonFeatuerListALL");
	}
	
	
	/*********************** Mo_cluster_node_feature table ********************/

	public List<HashMap<String,Object>> selectClusterNodeFeatureListAll() {
		return sqlSessionTemplate.selectList("featureMapper.selectClusterNodeFeatureListAll");
	}
	
	public List<HashMap<String,Object>> selectClusterFeatureListAll() {
		return sqlSessionTemplate.selectList("featureMapper.selectClusterFeatureListAll");
	}
	
	public void updateClusterNodeAutoFeature(ClusterNodeFeature vo) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		
		String autoFeature = null ;
		try {
			autoFeature = JSONUtil.getJsonstringFromObject(vo);
		} catch (JsonProcessingException e) {
		}
		map.put("clUid", vo.getClUid());
		map.put("nm"   , vo.getNodeName());
		map.put("autoFeature", autoFeature);
		
		sqlSessionTemplate.update("featureMapper.updateClusterNodeAutoFeature",map);
		map.clear();
	}
}

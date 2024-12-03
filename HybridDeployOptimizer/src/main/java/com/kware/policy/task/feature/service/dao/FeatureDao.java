package com.kware.policy.task.feature.service.dao;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.kware.policy.task.feature.service.vo.ClusterNodeFeature;
import com.kware.policy.task.feature.service.vo.CommonFeatureBase;

@Repository
public class FeatureDao {
	
	@Autowired
	@Qualifier("sqlSessionTemplate")
	SqlSessionTemplate sqlSessionTemplate;

	/*********************** Mo_common_feature_base table ********************/
	public List<CommonFeatureBase> selectFeatureBaseListALL() {
		return sqlSessionTemplate.selectList("featureMapper.selectFeatureBaseListALL");
	}
	
	
	/*********************** Mo_cluster_node_feature table ********************/

	public List<ClusterNodeFeature> selectClusterNodeFeatureListAll() {
		return sqlSessionTemplate.selectList("selectClusterNodeFeatureListAll");
	}
	
	public void insertClusterNodeFeature(ClusterNodeFeature vo) {
		sqlSessionTemplate.insert("insertClusterNodeFeature",vo);
	}
	
	public void insertClusterNodeFeatureHistory(ClusterNodeFeature vo) {
		sqlSessionTemplate.update("insertClusterNodeFeatureHistory",vo);
	}
}

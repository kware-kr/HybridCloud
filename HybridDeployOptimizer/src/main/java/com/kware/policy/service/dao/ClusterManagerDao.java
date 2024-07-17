package com.kware.policy.service.dao;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.kware.policy.service.vo.Cluster;
import com.kware.policy.service.vo.ClusterNode;
import com.kware.policy.service.vo.ClusterWorkload;

@Repository //@Repository("이름")으로 처리하고 @Resource(name="이름") 주입한다.
public class ClusterManagerDao {

	@Autowired
	@Qualifier("sqlSessionTemplate")
	SqlSessionTemplate sqlSessionTemplate;

	/************************ mo_cluster ***********************************/
	
	public void updateClusterAndInsertHistory(Cluster cluster) {
		int cnt = selectClusterCount(cluster); //hash값이 같은 데이터를 찾아서 있으면 변경안된것이고, 없으면 데이터가 변경된 것임  
		if(cnt == 0) { // 없거나, 변경되었거나(hash_val)
			insertHistoryFromCluster(cluster);//기존것 백업받고
			updateCluster(cluster);			
		}else {
			; //변경이 안되어서 처리안함
		}
	}
	
	
	public int insertCluster(Cluster cluster) {
		return sqlSessionTemplate.insert("clusterManagerMapper.insertCluster", cluster);
	}

	public int updateCluster(Cluster cluster) {
		return sqlSessionTemplate.update("clusterManagerMapper.updateCluster", cluster);
	}

	public int deleteCluster(Cluster cluster) {
		return sqlSessionTemplate.delete("clusterManagerMapper.deleteCluster", cluster);
	}

	public List selectClusterList(Cluster cluster) {
		return sqlSessionTemplate.selectList("clusterManagerMapper.selectClusterList", cluster);
	}
	
	public Cluster selectCluster(Cluster cluster) {
		return sqlSessionTemplate.selectOne("clusterManagerMapper.selectCluster", cluster);
	}

	public int selectClusterCount(Cluster cluster) {
		return sqlSessionTemplate.selectOne("clusterManagerMapper.selectClusterCount", cluster);
	}
	
	/************************ mo_cluster_node ***********************************/
	
	public void updateClusterNodeAndInsertHistory(ClusterNode node) {
		int cnt = selectClusterNodeCount(node);
		if(cnt == 0) { // 없거나, 변경되었거나(hash_val)		
			insertHistoryFromClusterNode(node);
			updateClusterNode(node);
		}else {
			; //변경이 안되어서 처리안함
		}
	}
	
	public int insertClusterNode(ClusterNode node) {
		return sqlSessionTemplate.insert("clusterManagerMapper.insertClusterNode", node);
	}

	public int updateClusterNode(ClusterNode node) {
		return sqlSessionTemplate.update("clusterManagerMapper.updateClusterNode", node);
	}

	public int deleteClusterNode(ClusterNode node) {
		return sqlSessionTemplate.delete("clusterManagerMapper.deleteClusterNode", node);
	}

	public List selectClusterNodeList(ClusterNode node) {
		return sqlSessionTemplate.selectList("clusterManagerMapper.selectClusterNodeList", node);
	}
	
	public ClusterNode selectClusterNode(ClusterNode node) {
		return sqlSessionTemplate.selectOne("clusterManagerMapper.selectClusterNode", node);
	}

	public int selectClusterNodeCount(ClusterNode node) {
		return sqlSessionTemplate.selectOne("clusterManagerMapper.selectClusterNodeCount", node);
	}

/************************ mo_cluster_workload ***********************************/
	
	public void updateClusterWorkloadAndInsertHistory(ClusterWorkload workload) {
		int cnt = selectClusterWorkloadCount(workload);
		if(cnt == 0) { // 없거나, 변경되었거나(hash_val)		
			insertHistoryFromClusterWorkload(workload);
			updateClusterWorkload(workload);
		}else {
			; //변경이 안되어서 처리안함
		}
	}
	
	public int insertClusterWorkload(ClusterWorkload workload) {
		return sqlSessionTemplate.insert("clusterManagerMapper.insertClusterWorkload", workload);
	}

	public int updateClusterWorkload(ClusterWorkload workload) {
		return sqlSessionTemplate.update("clusterManagerMapper.updateClusterWorkload", workload);
	}

	public int deleteClusterWorkload(ClusterWorkload workload) {
		return sqlSessionTemplate.delete("clusterManagerMapper.deleteClusterWorkload", workload);
	}

	public List selectClusterWorkloadList(ClusterWorkload workload) {
		return sqlSessionTemplate.selectList("clusterManagerMapper.selectClusterWorkloadList", workload);
	}
	
	public ClusterNode selectClusterWorkload(ClusterWorkload workload) {
		return sqlSessionTemplate.selectOne("clusterManagerMapper.selectClusterWorkload", workload);
	}

	public int selectClusterWorkloadCount(ClusterWorkload workload) {
		return sqlSessionTemplate.selectOne("clusterManagerMapper.selectClusterWorkloadCount", workload);
	}
	
	/************************ mo_cluster_history ***********************************/

	public int insertHistoryFromCluster(Cluster cluster) {
		return sqlSessionTemplate.insert("clusterManagerMapper.insertHistoryFromCluster", cluster);
	}

	public int insertHistoryFromClusterNode(ClusterNode node) {
		return sqlSessionTemplate.insert("clusterManagerMapper.insertHistoryFromClusterNode", node);
	}
	
	public int insertHistoryFromClusterWorkload(ClusterWorkload workload) {
		return sqlSessionTemplate.insert("clusterManagerMapper.insertHistoryFromClusterWorkload", workload);
	}

}

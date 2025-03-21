package com.kware.policy.task.collector.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.kware.policy.task.collector.service.dao.ClusterManagerDao;
import com.kware.policy.task.collector.service.vo.Cluster;
import com.kware.policy.task.collector.service.vo.ClusterNode;
import com.kware.policy.task.collector.service.vo.ClusterWorkload;

import lombok.extern.slf4j.Slf4j;

/**
 * 통합시스템에서 제공하는 API를 통해서 클러스터의 리스트 및 각 클러스터에 등록된 노드의 정보, 각 노드에서 배포된 Workload리스트를
 * 수집하는 API 클러스터 리스트 등록, 클러스터 노드 등록
 */
@Slf4j
@Service
public class ClusterManagerService {

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Autowired
	private ClusterManagerDao clusterManagerDao;

	/******************** Cluster *********************/
	public void updateClusterAndInsertHistory(Cluster cluster) {
		clusterManagerDao.updateClusterAndInsertHistory(cluster);
	}

	public int insertCluster(Cluster cluster) {
		return clusterManagerDao.insertCluster(cluster);
	}

	public int updateCluster(Cluster cluster) {
		return clusterManagerDao.updateCluster(cluster);
	}

	public int deleteCluster(Cluster cluster) {
		return clusterManagerDao.deleteCluster(cluster);
	}

	public List selectClusterList(Cluster cluster) {
		return clusterManagerDao.selectClusterList(cluster);
	}

	public Cluster selectCluster(Cluster cluster) {
		return clusterManagerDao.selectCluster(cluster);
	}

	/******************** ClusterNode *********************/
	public void updateClusterNodeAndInsertHistory(ClusterNode node) {
		clusterManagerDao.updateClusterNodeAndInsertHistory(node);
	}

	public int insertClusterNode(ClusterNode node) {
		return clusterManagerDao.insertClusterNode(node);
	}

	public int updateClusterNode(ClusterNode node) {
		return clusterManagerDao.updateClusterNode(node);
	}

	public int deleteClusterNode(ClusterNode node) {
		return clusterManagerDao.deleteClusterNode(node);
	}

	public List selectClusterNodeList(ClusterNode node) {
		return clusterManagerDao.selectClusterNodeList(node);
	}

	public ClusterNode selectClusterNode(ClusterNode node) {
		return clusterManagerDao.selectClusterNode(node);
	}

	/******************** ClusterWrokload *********************/

	public void updateClusterWorkloadAndInsertHistory(ClusterWorkload workload) {
		clusterManagerDao.updateClusterWorkloadAndInsertHistory(workload);
	}

	public int insertClusterWorkload(ClusterWorkload workload) {
		return clusterManagerDao.insertClusterWorkload(workload);
	}

	public int updateClusterWorkload(ClusterWorkload workload) {
		return clusterManagerDao.updateClusterWorkload(workload);
	}

	public int deleteClusterWorkload(ClusterWorkload workload) {
		return clusterManagerDao.deleteClusterWorkload(workload);
	}

	public List selectClusterWorkloadList(ClusterWorkload workload) {
		return clusterManagerDao.selectClusterWorkloadList(workload);
	}

	public ClusterWorkload selectClusterWorkload(ClusterWorkload workload) {
		return clusterManagerDao.selectClusterWorkload(workload);
	}

	/******************** Cluster history *********************/
	//DAO에만 있음
	
	
	
	//웹소켓을 통해서 구독자에게 신규워크로드 등록을 알려준다.
	public void sendNewClousterWorkload(String _mlId) {
	   try {
		   messagingTemplate.convertAndSend("/topic/newClusterWorkload", _mlId);
	   }catch(Exception e) {
		   log.error("WebSocket Send Error: {}", _mlId, e);
	   }
	}
}
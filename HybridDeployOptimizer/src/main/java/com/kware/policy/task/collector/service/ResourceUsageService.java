package com.kware.policy.task.collector.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kware.policy.task.collector.service.dao.ResourceUsageDao;
import com.kware.policy.task.collector.service.vo.ClusterWorkload;
import com.kware.policy.task.collector.service.vo.ResourceUsageNode;
import com.kware.policy.task.collector.service.vo.ResourceUsagePod;

/**
 * 통합시스템에서 제공하는 API를 통해서 클러스터의 리스트 및 각 클러스터에 등록된 노드의 정보, 각 노드에서 배포된 Workload리스트를 수집하는 API
 * 클러스터 리스트 등록, 클러스터 노드 등록
 */
//@Slf4j
@Service
public class ResourceUsageService {

	@Autowired
	private ResourceUsageDao defaultDao;
	
	/************************ mo_resource_usage_node ***********************************/
	
	public int insertResourceUsageNode(ResourceUsageNode vo) {
		return defaultDao.insertResourceUsageNode(vo);
	}
	
	/**
	 *  입력은 요청이 파라미터: APIPagedRequest을 사용할등
	 * @param vo
	 * @return
	 */
	public List selectResourceUsageNodeList(ResourceUsageNode vo) {
		return defaultDao.selectResourceUsageNodeList(vo);
	}
	
	
	/************************ mo_resource_usage_pod ***********************************/
	
	public int insertResourceUsagePod(ResourceUsagePod vo) {
		return defaultDao.insertResourceUsagePod(vo);
	}
	
	/**
	 *  입력은 요청이 파라미터: APIPagedRequest을 사용할등
	 * @param vo
	 * @return
	 */
	public List selectResourceUsagePodList(ResourceUsagePod vo) {
		return defaultDao.selectResourceUsagePodList(vo);
	}
	
	
	public int updateClusterWorkloadToUsage_info(String mlId) {
		return defaultDao.updateClusterWorkloadToUsage_info(mlId);
	}
}
package com.kware.policy.task.collector.service.dao;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.kware.policy.task.collector.service.vo.ResourceUsageNode;
import com.kware.policy.task.collector.service.vo.ResourceUsagePod;

@Repository //@Repository("이름")으로 처리하고 @Resource(name="이름") 주입한다.
@SuppressWarnings("rawtypes")
public class ResourceUsageDao {

	@Autowired
	@Qualifier("sqlSessionTemplate")
	SqlSessionTemplate sqlSessionTemplate;

	/************************ mo_resource_usage_node ***********************************/
	
	public int insertResourceUsageNode(ResourceUsageNode vo) {
		return sqlSessionTemplate.insert("resourceUsageMapper.insertResourceUsageNode", vo);
	}
	
	/**
	 *  입력은 요청이 파라미터: APIPagedRequest을 사용할등
	 * @param vo
	 * @return
	 */
	public List selectResourceUsageNodeList(ResourceUsageNode vo) {
		return sqlSessionTemplate.selectList("resourceUsageMapper.selectResourceUsageNodeList", vo);
	}
	
	
	/************************ mo_resource_usage_pod ***********************************/
	
	public int insertResourceUsagePod(ResourceUsagePod vo) {
		return sqlSessionTemplate.insert("resourceUsageMapper.insertResourceUsagePod", vo);
	}
	
	/**
	 *  입력은 요청이 파라미터: APIPagedRequest을 사용할등
	 * @param vo
	 * @return
	 */
	public List selectResourceUsagePodList(ResourceUsagePod vo) {
		return sqlSessionTemplate.selectList("resourceUsageMapper.selectResourceUsagePodList", vo);
	}
	

}

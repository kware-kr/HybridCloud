package com.kware.policy.task.collector.service.dao;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.kware.policy.task.collector.service.vo.Cluster;
import com.kware.policy.task.collector.service.vo.PromQL;
import com.kware.policy.task.collector.service.vo.PromQLResult;

@Repository //@Repository("이름")으로 처리하고 @Resource(name="이름") 주입한다.
public class PromQLDao  {
	
	@Autowired 
	@Qualifier("sqlSessionTemplate")
	SqlSessionTemplate sqlSessionTemplate;
	
	public int insertPromqlResult(PromQLResult vo) {
		return sqlSessionTemplate.insert("promQLCollectMapper.insertPromqlResult", vo);		
	}
	
	/**
	 * 클러스터와 관련된 metric query list;
	 * @param vo
	 * @return
	 */
	public List<Integer> selectPromqlIdList(PromQL vo) {
		return sqlSessionTemplate.selectList("promQLCollectMapper.selectPromqlIdList", vo);		
	}
	
	/**
	 * 클러스터와 상관없는 metric query list
	 * @return
	 */
	public List<PromQL> selectPromqlListAll() {
		return sqlSessionTemplate.selectList("promQLCollectMapper.selectPromqlListAll");		
	}
	
	public List<Cluster> selectClusterList() {
		return sqlSessionTemplate.selectList("promQLCollectMapper.selectClusterList");		
	}
	
	
	
	
	/* 참고로 남겨놓으니 지우지 말자...
	@Autowired
	private ApplicationContext applicationContext;
	
	
	public void aaa() {
		applicationContext.getBean("dolhpin-sqlSessionTemplate");
	}*/
}

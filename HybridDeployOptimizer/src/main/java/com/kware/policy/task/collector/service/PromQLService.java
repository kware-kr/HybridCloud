package com.kware.policy.task.collector.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kware.policy.task.collector.service.dao.PromQLDao;
import com.kware.policy.task.collector.service.vo.Cluster;
import com.kware.policy.task.collector.service.vo.PromQL;
import com.kware.policy.task.collector.service.vo.PromQLResult;


//@Slf4j
@Service
public class PromQLService {
	
	@Autowired
	protected PromQLDao dao;


	public List<Cluster> selectClusterList() {
		return dao.selectClusterList();
	}
	
	/**
	 * 클러스터와 관련된 metric query list;
	 * @param vo
	 * @return
	 */
	public List<Integer> selectPromqlIdList(PromQL vo) {
		return dao.selectPromqlIdList(vo);
	}
	
	/**
	 * 전체 metric query list;
	 * @param vo
	 * @return
	 */
	public List<PromQL> selectPromqlListAll() {
		return dao.selectPromqlListAll();
	}
	
	public int insertPromqlResult(PromQLResult vo) {
		return dao.insertPromqlResult(vo);
	}

}
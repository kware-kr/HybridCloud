package com.kware.policy.task.collector.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
	
	@Value("${hybrid.policy.exclude-namespaces}")
	private String excludeNamespaces;
	
	private Map<String, String> paramMap = new HashMap<String,String>();
	
	@PostConstruct
	private void init() {
		paramMap.put("excludeNamespaces", excludeNamespaces);
	}
	


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
		return dao.selectPromqlListAll(paramMap);
	}
	
	public int insertPromqlResult(PromQLResult vo) {
		return dao.insertPromqlResult(vo);
	}

}
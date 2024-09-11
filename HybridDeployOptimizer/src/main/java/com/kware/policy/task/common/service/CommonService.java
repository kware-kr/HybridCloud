package com.kware.policy.task.common.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kware.policy.task.common.service.dao.CommonDao;
import com.kware.policy.task.common.service.vo.CommonConfigGroup;


//@Slf4j
@Service
public class CommonService {
	
	//{{db관련 서비스
	@Autowired
	protected CommonDao dao;

	public List<?> selectCommonConfigGroupList() {
		return dao.selectCommonConfigGroupList();
	}

	public CommonConfigGroup selectCommonConfigGroup(CommonConfigGroup.ConfigName cfgName) {
		return dao.selectCommonConfigGroup(cfgName);
	}
	//}}db 관련 서비스
	
	
}
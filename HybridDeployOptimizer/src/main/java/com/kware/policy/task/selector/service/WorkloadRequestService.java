package com.kware.policy.task.selector.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kware.policy.task.APIConstant;
import com.kware.policy.task.selector.service.dao.WorkloadRequestDao;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Request;
import com.kware.policy.task.selector.service.vo.WorkloadResponse.Response;


//@Slf4j
@Service
public class WorkloadRequestService {
	public final static String interface_version = APIConstant.POLICY_INTERFACE_VERSION;
	
	//{{db관련 서비스
	@Autowired
	protected WorkloadRequestDao dao;

	public int insertMoUserRequest(Request vo) {
		return dao.insertMoUserRequest(vo);
	}

	public int insertMoUserResponse(Response vo) {
		return dao.insertMoUserResponse(vo);
	}
	//}}db 관련 서비스
	
	//{{노드 셀렉터
	
	//}}

}
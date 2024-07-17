package com.kware.policy.task.selector.service.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.kware.policy.task.selector.service.vo.WorkloadRequest.Request;
import com.kware.policy.task.selector.service.vo.WorkloadResponse.Response;

@Repository //@Repository("이름")으로 처리하고 @Resource(name="이름") 주입한다.
public class WorkloadRequestDao {
	@Autowired
	@Qualifier("sqlSessionTemplate")
	SqlSessionTemplate sqlSessionTemplate;

	/************************ mo_user_request ***********************************/
	
	
	public int insertMoUserRequest(Request vo) {
		return sqlSessionTemplate.insert("mlRequetResponseMapper.insertMoUserRequest", vo);
	}

	public int insertMoUserResponse(Response vo) {
		return sqlSessionTemplate.insert("mlRequetResponseMapper.insertMoUserResponse", vo);
	}
}

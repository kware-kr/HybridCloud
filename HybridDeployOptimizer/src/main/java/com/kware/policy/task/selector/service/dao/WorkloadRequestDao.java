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
	
	
	/**
	 * 최초 요청 입력 저장
	 * @param vo
	 * @return
	 */
	public int insertMoUserRequest(Request vo) {
		return sqlSessionTemplate.insert("mlRequetResponseMapper.insertMoUserRequest", vo);
	}
	
	/**
	 * 워크로드 배포 통지 입력 저장
	 * @param vo
	 * @return
	 */
	public int updateMoUserRequest_noti(Request vo) {
		return sqlSessionTemplate.insert("mlRequetResponseMapper.updateMoUserRequest_noti", vo);
	}
	
	/**
	 * 워크로드 완료시간 입력 저장
	 * @param mlId
	 * @return
	 */
	public int updateMoUserRequest_complete(String mlId) {
		return sqlSessionTemplate.insert("mlRequetResponseMapper.updateMoUserRequest_complete", mlId);
	}

	/**
	 * 요청에 대한 응답 저장
	 * @param vo
	 * @return
	 */
	public int insertMoUserResponse(Response vo) {
		return sqlSessionTemplate.insert("mlRequetResponseMapper.insertMoUserResponse", vo);
	}
}

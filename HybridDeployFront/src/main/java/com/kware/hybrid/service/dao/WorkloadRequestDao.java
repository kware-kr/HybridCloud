package com.kware.hybrid.service.dao;


import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import com.kware.hybrid.service.vo.WorkloadRequestVO;

@Repository
public class WorkloadRequestDao {
   
	private final SqlSession sqlSession;

    public WorkloadRequestDao(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

   

    public WorkloadRequestVO selectRunningWrokloadByMlid(String mlId) {
        return sqlSession.selectOne("workloadRequestMapper.selectRunningWrokloadByMlid", mlId);
    }

    public List<WorkloadRequestVO> selectRunningWrokload() {
        return sqlSession.selectList("workloadRequestMapper.selectRunningWrokload");
    }
    
    
}

package com.kware.hybrid.service.dao;


import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import com.kware.hybrid.service.vo.DashboardVO;

@Repository
public class DashboardDao {
   
	private final SqlSession sqlSession;

    public DashboardDao(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    public List<DashboardVO> selectDashboard() {
        return sqlSession.selectList("dashboardMapper.selectDashboard");
    }
    
    public List<DashboardVO> selectClusters() {
        return sqlSession.selectList("dashboardMapper.selectClusters");
    }
    
}

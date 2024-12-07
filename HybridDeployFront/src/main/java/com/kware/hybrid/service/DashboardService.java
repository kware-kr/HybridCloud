package com.kware.hybrid.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kware.hybrid.service.dao.DashboardDao;
import com.kware.hybrid.service.vo.DashboardVO;

@Service
public class DashboardService {
    private final DashboardDao dao;

    public DashboardService(DashboardDao dao) {
        this.dao = dao;
    }

    public List<DashboardVO> getDashboard() {
        return dao.selectDashboard();
    }
    
    public List<DashboardVO> getClusters() {
        return dao.selectClusters();
    }
}

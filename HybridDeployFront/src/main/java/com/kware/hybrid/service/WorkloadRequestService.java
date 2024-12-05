package com.kware.hybrid.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kware.hybrid.service.dao.WorkloadRequestDao;
import com.kware.hybrid.service.vo.WorkloadRequestVO;

@Service
public class WorkloadRequestService {
    private final WorkloadRequestDao dao;

    public WorkloadRequestService(WorkloadRequestDao dao) {
        this.dao = dao;
    }

    
    public WorkloadRequestVO getRunningWrokloadByMlid(String mlId) {
        return dao.selectRunningWrokloadByMlid(mlId);
    }

    public List<WorkloadRequestVO> getRunningWrokload() {
        return dao.selectRunningWrokload();
    }    
}

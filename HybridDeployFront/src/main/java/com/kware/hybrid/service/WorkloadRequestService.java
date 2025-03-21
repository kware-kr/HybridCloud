package com.kware.hybrid.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kware.hybrid.service.dao.WorkloadRequestDao;
import com.kware.hybrid.service.vo.ResourcePodUsageVO;
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

    public List<WorkloadRequestVO> getRunningWrokload(WorkloadRequestVO vo) {
        return dao.selectRunningWrokload(vo);
    }
    
    public Integer getRunningWrokloadCount(WorkloadRequestVO vo) {
        return dao.selectRunningWrokloadCount(vo);
    }
    
    
    public List<ResourcePodUsageVO> getPodUsage (ResourcePodUsageVO vo) {
        return dao.selectPodUsage(vo);
    }
}

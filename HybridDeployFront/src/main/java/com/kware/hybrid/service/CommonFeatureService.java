package com.kware.hybrid.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kware.hybrid.service.dao.CommonFeatureDao;
import com.kware.hybrid.service.vo.ClusterNodeFeatureVO;
import com.kware.hybrid.service.vo.CommonFeatureVO;

@Service
public class CommonFeatureService {
    private final CommonFeatureDao commonFeatureDao;

    public CommonFeatureService(CommonFeatureDao commonFeatureDao) {
        this.commonFeatureDao = commonFeatureDao;
    }

    public int insertCommonFeature(CommonFeatureVO commonFeature) {
        return commonFeatureDao.insertCommonFeature(commonFeature);
    }

    public int updateCommonFeature(CommonFeatureVO commonFeature) {
        return commonFeatureDao.updateCommonFeature(commonFeature);
    }

    public int deleteCommonFeature(String feaName, String feaSubName) {
        return commonFeatureDao.deleteCommonFeature(feaName, feaSubName);
    }

    public CommonFeatureVO getCommonFeatureByKey(String feaName, String feaSubName) {
        return commonFeatureDao.selectCommonFeatureByKey(feaName, feaSubName);
    }

    public List<CommonFeatureVO> getAllCommonFeatures(String feaName) {
        return commonFeatureDao.selectAllCommonFeatures(feaName);
    }
    
    
    // {{mo_cluster 클러스터 특성관련
    public List<ClusterNodeFeatureVO> getAllClusterFeatures() {
        return commonFeatureDao.selectAllClusterFeatures();
    }
    
    public int updateClusterFeature(ClusterNodeFeatureVO vo) {
        return commonFeatureDao.updateClusterFeature(vo);
    }
    // }}mo_cluster 클러스터 특성관련
    
 // {{mo_cluster_node 클러스터 특성관련
    public List<ClusterNodeFeatureVO> getAllClusterNodeFeatures() {
        return commonFeatureDao.selectAllClusterNodeFeatures();
    }
    
    public int updateClusterNodeFeature(ClusterNodeFeatureVO vo) {
        return commonFeatureDao.updateClusterNodeFeature(vo);
    }
    // }}mo_cluster 클러스터 특성관련
}

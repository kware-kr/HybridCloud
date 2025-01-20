package com.kware.hybrid.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kware.hybrid.service.dao.CommonFeatureDao;
import com.kware.hybrid.service.vo.ClusterNodeFeatureVO;
import com.kware.hybrid.service.vo.CommonFeatureVO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CommonFeatureService {
	
	private RestTemplate restTemplate;

    @Value("${hybrid.proxy.url:null}")
    private String targetUrl;
    
    private String proxySettingChangeUrl = null;
    
    @PostConstruct
    private void init() {
    	this.restTemplate = new RestTemplate();
    	if(targetUrl.endsWith("/"))
    		proxySettingChangeUrl = targetUrl + "interface/common/setting/change";
    	else
    		proxySettingChangeUrl = targetUrl + "/interface/common/setting/change";
    	
    	//proxySettingChangeUrl = proxySettingChangeUrl.replaceAll("//", "/");
    }
    
    private final CommonFeatureDao commonFeatureDao;

    public CommonFeatureService(CommonFeatureDao commonFeatureDao) {
        this.commonFeatureDao = commonFeatureDao;
    }

    public int insertCommonFeature(CommonFeatureVO commonFeature) {
        int rst = commonFeatureDao.insertCommonFeature(commonFeature);
        sendSettingChange("feature");
        return rst;
    }

    public int updateCommonFeature(CommonFeatureVO commonFeature) {
    	int rst = commonFeatureDao.updateCommonFeature(commonFeature);
    	sendSettingChange("feature");
    	return rst;
    }

    public int deleteCommonFeature(String feaName, String feaSubName) {
    	int rst = commonFeatureDao.deleteCommonFeature(feaName, feaSubName);
    	sendSettingChange("feature");
    	return rst;
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
    	int rst = commonFeatureDao.updateClusterFeature(vo);
    	sendSettingChange("cluster");
    	return rst;
    }
    // }}mo_cluster 클러스터 특성관련
    
 // {{mo_cluster_node 클러스터 특성관련
    public List<ClusterNodeFeatureVO> getAllClusterNodeFeatures() {
        return commonFeatureDao.selectAllClusterNodeFeatures();
    }
    
    public int updateClusterNodeFeature(ClusterNodeFeatureVO vo) {
    	int rst = commonFeatureDao.updateClusterNodeFeature(vo);
    	sendSettingChange("node");
    	return rst;
    }
    // }}mo_cluster 클러스터 특성관련
    
	//설정값이 변경되면 백엔드 서버에 변경사실을 전달한다.
	private void sendSettingChange(String gubun) {
		new Thread(() -> {
			try {
		        String url = proxySettingChangeUrl+ "?g=" + gubun; 
		        restTemplate.getForObject(url, Void.class);
			}catch(Exception e) {
				log.error("API call to notify the Optimizer server of configuration changes failed. Error:", e);
			}
		}).start();
    }
}

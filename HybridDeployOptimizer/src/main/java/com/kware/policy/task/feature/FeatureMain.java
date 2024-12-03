package com.kware.policy.task.feature;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kware.policy.task.collector.service.ClusterManagerService;
import com.kware.policy.task.collector.service.PromQLService;
import com.kware.policy.task.collector.service.ResourceUsageService;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.feature.finder.PerformanceLevelFinder;
import com.kware.policy.task.feature.service.FeatureService;
import com.kware.policy.task.feature.service.vo.ClusterNodeFeature;
import com.kware.policy.task.feature.service.vo.CommonFeatureBase;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 */

@Slf4j
@EnableScheduling // 스케줄링 활성화
@Component
public class FeatureMain {

	// @Autowired
	public FeatureMain() {
	}

	@Autowired
	private FeatureService ftService;

	@Autowired
	private PromQLService pqService;

	@Autowired
	private ClusterManagerService cmService;

	@Autowired
	private ResourceUsageService ruService;

	final QueueManager qm = QueueManager.getInstance();

	APIQueue apiQ = qm.getApiQ();
	PromQueue promQ = qm.getPromQ();

	// 향후 외부에서 이 데이터를 변경하면 새롭게 데이터를 DB에서 읽어 올 수 있도록
	boolean isChangeFeatureBase = true;

	public void setChangeFeatureBase(boolean isChangeFeatureBase) {
		this.isChangeFeatureBase = isChangeFeatureBase;
	}

	private Map<String, Object> featureBaseMap = new HashMap<String, Object>();
	private Map<Integer, ClusterNodeFeature> nodeFeatueMap = new HashMap<Integer, ClusterNodeFeature>();

	@PostConstruct
	private void init() {
		init_feature_base();
		init_node_feature();
	}

	/**
	 * 특성 기준정보를 가져온다.
	 */
	//@Scheduled(cron = "0 * * * * *") // 1분에 한번 처리할까? 아니면 뭔가 값이 변경되면 처리할까.
	public void getFeatureBase() {
		init_feature_base();

		// 1. gen_level(노드의 일반 성능을 설정한다.)
		try {
			process_gen_level();
		} catch (IOException e) {
			log.error("process_gen_level 생성 에러",e);
		}
	}

	private void init_feature_base() {
		if (isChangeFeatureBase) {
			List<CommonFeatureBase> list = ftService.selectFeatureBaseListALL();
			for (CommonFeatureBase b : list) {
				featureBaseMap.put(b.getCfgName(), b.getCfgContent());
			}
			if (list != null)
				list.clear();

			isChangeFeatureBase = false;
		}
	}
	private void init_node_feature() {
		List<ClusterNodeFeature> list2 = ftService.selectClusterNodeFeatureListAll();
		for (ClusterNodeFeature b : list2) {
			nodeFeatueMap.put(b.getNoUid(), b);
		}
		if (list2 != null)
			list2.clear();
	}
	
	private void process_gen_level() throws IOException {
		String jsonstring = (String)featureBaseMap.get(FeatureKey.GEN_LEVEL);
		PerformanceLevelFinder finder = new PerformanceLevelFinder(jsonstring);
		
		apiQ.getApiClusterNodeMap();
		
		
	}
	
	
	
	public enum FeatureKey {
	    GPU_LEVEL("gpu_level"),
	    PRIORITY_CLASS("priorityClass"),
	    CLOUD_TYPE("cloud_type"),
	    WORKLOAD_FEATURE("workload_feature"),
	    WORKLOAD_TYPE("workload_type"),
	    SEC_LEVEL("sec_level"),
	    GEN_LEVEL("gen_level"),
	    WORKLOAD_DEPLOYMENT_STAGE("workload_deployment_stage");

	    private final String key;

	    FeatureKey(String key) {
	        this.key = key;
	    }

	    public String getKey() {
	        return key;
	    }

	    public static FeatureKey fromKey(String key) {
	        for (FeatureKey feature : FeatureKey.values()) {
	            if (feature.key.equals(key)) {
	                return feature;
	            }
	        }
	        throw new IllegalArgumentException("Invalid Feature Key: " + key);
	    }
	}
	

}

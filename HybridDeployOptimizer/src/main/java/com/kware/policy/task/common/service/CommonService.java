package com.kware.policy.task.common.service;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kware.common.util.RestTemplateUtil;
import com.kware.policy.task.common.service.dao.CommonDao;
import com.kware.policy.task.common.service.dao.CommonEventDao;
import com.kware.policy.task.common.service.vo.CommonConfigGroup;
import com.kware.policy.task.common.service.vo.CommonEvent;
import com.kware.policy.task.scalor.service.vo.PodScalingInfo;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;


//@Slf4j
@Service
public class CommonService {
	
	//{{db관련 서비스
	@Autowired
	protected CommonDao dao;
	
	//gpu의 개별 스코어를 등록함
	HashMap<String, Double> gpuScoreMap = new HashMap<String, Double>();
	private static final int MAX_COUNT = 100;
	private static final AtomicInteger counter = new AtomicInteger(0); 
	

	public List<?> getCommonConfigGroupList() {
		return dao.selectCommonConfigGroupList();
	}

	public List<CommonConfigGroup> getCommonConfigGroup(String feaName) {
		return dao.selectCommonConfigGroup(feaName);
	}
	
	public CommonConfigGroup getCommonConfigGroupSub(String feaName, String feaSubName) {
		return dao.selectCommonConfigGroupSub(feaName, feaSubName);
	}
	
	public Double getCommonGpuScore(String product) {
		Double score = gpuScoreMap.get(product);
		if(score == null) {
			score = dao.selectCommonGpuScore(product);
			if(score != null) {
				gpuScoreMap.put(product, score);
			}
		}else if(score == 0) {
			int currentCount = counter.incrementAndGet();
			if (currentCount == MAX_COUNT) {
				counter.set(0); // 다시 0으로 초기화
				score = dao.selectCommonGpuScore(product);
				if(score != null) {
					score = 0.;
				}
				gpuScoreMap.put(product, score);
			}
		}
		return score;
	}
	//}}db 관련 서비스
	
	@Autowired
	protected CommonEventDao eventDAO;
	
	public void createEvent(String name, String type, String desc ){
		CommonEvent event = new CommonEvent(name, type, desc);
		this.createEvent(event);
	}

    public void createEvent(CommonEvent event) {
        eventDAO.insertEvent(event);
    }

    public CommonEvent getEventById(int id) {
        return eventDAO.getEventById(id);
    }

    public List<CommonEvent> getAllEvents(CommonEvent event) {
        return eventDAO.getAllEvents(event);
    }
    
    /////////////////////////////////////////////////////////////////////////////////////////  
    
    @Value("${hybrid.request.url}")
    private String hybrid_request_url;  
    
    @Autowired
	protected RestTemplate restTemplate;
    
    RestTemplateUtil restTemplateUtil = null;
    
    @PostConstruct
    public void init() {
    	restTemplateUtil = new RestTemplateUtil(restTemplate);
    }
    
    public void requestScalingApiCall(PodScalingInfo psInfo, WorkloadRequest workloadRequest) {
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    }
}



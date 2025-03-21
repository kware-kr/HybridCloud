package com.kware.policy.task.scalor.service;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kware.common.util.JSONUtil;
import com.kware.common.util.RestTemplateUtil;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.common.constant.APIConstant;
import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.common.service.CommonService;
import com.kware.policy.task.scalor.service.dao.ScalingInfoDao;
import com.kware.policy.task.scalor.service.vo.NodeScalingInfo;
import com.kware.policy.task.scalor.service.vo.PodScalingInfo;
import com.kware.policy.task.scalor.service.vo.ScalingInfo;
import com.kware.policy.task.scalor.worker.PodWorker;
import com.kware.policy.task.selector.service.vo.NodeScalingInfoRequest;
import com.kware.policy.task.selector.service.vo.PodScalingRequet;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Container;

@Service
//@Slf4j
public class ScalingInfoService {
	private static final Logger log = LoggerFactory.getLogger("scale-log");
    private final ScalingInfoDao dao;
   
    public ScalingInfoService(ScalingInfoDao dao) {
		this.dao = dao;
	}

	/**
     * 신규 스케일링 정보 생성
     *
     * @param dto 저장할 정보
     * @return 삽입 결과 (영향받은 행 수)
     */
    @Transactional
    public int createMoScalingInfo(ScalingInfo dto) {
        int result = dao.insertScalingInfo(dto);
        return result;
    }

    /**
     * uid로 스케일링 정보 조회
     *
     * @param uid 고유 식별자
     * @return 조회된 MoScalingInfoDTO (없으면 null)
     */
    public ScalingInfo getScalingInfoByUid(Long uid) {
        return dao.selectScalingInfoByUid(uid);
    }

    /**
     * 모든 스케일링 정보 목록 조회
     *
     * @return 스케일링 정보 리스트
     */
    public List<ScalingInfo> getAllScalingInfos() {
        return dao.selectAllScalingInfos();
    }

    /**
     * 스케일링 정보 업데이트
     *
     * @param dto 업데이트할 정보
     * @return 업데이트된 행 수
     */
    @Transactional
    public int updateMoScalingInfo(ScalingInfo dto) {
        int result = dao.updateScalingInfo(dto);
        return result;
    }

    /**
     * uid로 스케일링 정보 삭제
     *
     * @param uid 삭제할 정보의 고유 식별자
     * @return 삭제된 행 수
     */
    @Transactional
    public int deleteScalingInfo(Long uid) {
        int result = dao.deleteScalingInfo(uid);
        return result;
    }
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    @Autowired
    private CommonService comService;
    
    @Value("${hybrid.request.url}")
    private String hybrid_request_url;  
    
    @Value("${hybrid.my.callback-url}")
    private String my_callback_url;  
    
    @Value("${hybrid.collector.portal-api.url}")
	private String api_base_url;
    
    @Value("${hybrid.collector.portal-api.authorization}")
	private String api_authorization_token;
    
    
    @Autowired
	protected RestTemplate restTemplate;
    
    RestTemplateUtil restTemplateUtil = null;
    HttpHeaders reDeployHeaders = null;
    HttpHeaders portalHeaders = null;
    
    @PostConstruct
    public void init() {
    	restTemplateUtil = new RestTemplateUtil(restTemplate);
    	
    	reDeployHeaders = new HttpHeaders();
    	reDeployHeaders.setContentType(MediaType.APPLICATION_JSON);
    	
    	portalHeaders = new HttpHeaders();
    	portalHeaders.setContentType(MediaType.APPLICATION_JSON);
    	portalHeaders.add(StringConstant.STR_Authorization, this.api_authorization_token);
    	
    }
    
    final long MEBIBYTE = 1024L * 1024L; // 1,048,576
    private Long bytesToMi(Long bytes) {
        long val =  bytes / MEBIBYTE;
        return (long)(Math.ceil(val /10.0) * 10);
    }
    
    private String getNumberString(Object number) {
    	if(number != null)
    		return number.toString();
    	else return null;
    }
    
    
    private String getFormmatCpu(Long val) {
    	if(val != null) {
    		Long rs = (long)(Math.ceil(val/10.0) * 10);
    		return rs.toString();
    	}
    	else return null;
    }
    
    
    public void requestPodScalingApiCall(PodScalingInfo psInfo, WorkloadRequest workloadRequest) {   
    	PodScalingRequet psRequest = new PodScalingRequet();

    	PromMetricPod promPod = psInfo.promMetricPod;
    	psRequest.setName(promPod.getMlId());
    	psRequest.setCluster(promPod.getClUid().toString());

    	//{{기존 요청정보를 스케일링 요청정보의 입력으로 초기화 및 해당하는 파드 스케일링 처리.
        // Containers 리스트 생성: 
    	Integer mlContainerNameIdx = promPod.getMlContainerNameIdx();
    	String  mlContainerName = null;
        List<PodScalingRequet.Container> psContainers = new ArrayList<>();        
        List<Container> wlReqContainers = workloadRequest.getRequest().getContainers();
        for(WorkloadRequest.Container wlC : wlReqContainers) {
        	//원래 나오면 안되는데 가끔 나오는 경우가 있네.
        	if(mlContainerNameIdx == null) {
        		if(promPod.getPod().contains(wlC.getName())) {
        			mlContainerNameIdx = wlC.getNameIdx();
        			promPod.setMlContainerNameIdx(mlContainerNameIdx);
        		}
        	}
        	
        	WorkloadRequest.ResourceDetail wlLimits   = wlC.getResources().getLimits();
        	WorkloadRequest.ResourceDetail wlRequests = wlC.getResources().getRequests();
        	
        	PodScalingRequet.Container psC = new PodScalingRequet.Container();
        	psC.setName(wlC.getName());
        	
        	
        	//psInfo.promMetricPod.getPodUid()
        	
        	 // Resources 객체 생성
            PodScalingRequet.Resources psRes = new PodScalingRequet.Resources();

            String temp = null; 
            // requests(요청 스펙) 생성 스펙에는 모두 규정이 되어 있어서 숫자로만 보낸다.
            PodScalingRequet.ResourceDetail psRequests = new PodScalingRequet.ResourceDetail();
            
         // limits(최대 스펙) 생성
            PodScalingRequet.ResourceDetail psLimits = new PodScalingRequet.ResourceDetail();
            
            if(wlC.getNameIdx() == mlContainerNameIdx) {
            	mlContainerName = wlC.getName(); //최최 요청 컨테이너 이름
            	psInfo.pod_name = mlContainerName; 
            	Map<String, Long> rmap = psInfo.getNewRequestsMap();
            	
            	
            	// ========================= requests ==============================
            	Long obj = rmap.get(PodWorker.POD_CPU);
            	if(obj != null) {
            		temp = this.getFormmatCpu(obj);
	            	psRequests.setCpu(temp + "m");               // CPU 밀리코어
            	}else {
            		if(wlRequests.getCpu() != 0) {
    		            temp = this.getFormmatCpu((long)wlRequests.getCpu());
    		            psRequests.setCpu(temp + "m");               // CPU 밀리코어
                	}
            	}
            	
	            obj = rmap.get(PodWorker.POD_MEMORY);
	            if(obj != null) {
	            	temp = this.getNumberString(bytesToMi(obj));
	            	psRequests.setMemory(temp + "Mi");           // 메모리 MiB
            	}else {
            		if(wlRequests.getMemory() != 0) {
    		            temp = this.getNumberString(bytesToMi(wlRequests.getMemory()));
    		            psRequests.setMemory(temp + "Mi");           // 메모리 MiB
    	            }
            	}
	            
	            obj = rmap.get(PodWorker.POD_GPU);
	            if(obj != null) {
	            	temp = this.getNumberString(obj);
	            	psRequests.setGpu(temp);                 // GPU 1개
            	}else {
            		if(wlRequests.getGpu() != 0) {
    		            temp = this.getNumberString(wlRequests.getGpu());
    		            psRequests.setGpu(temp);                 // GPU 1개
    	            }
            	}
	            
	            obj = rmap.get(PodWorker.POD_DISK);
	            if(obj != null) {
	            	temp = this.getNumberString(obj);
	            	psRequests.setEphemeralStorage(temp); // 스토리지
            	}else {
    	            if(wlRequests.getEphemeralStorage() != 0) {
    		            temp = this.getNumberString(wlRequests.getEphemeralStorage());
    		            psRequests.setEphemeralStorage(temp); // 임시 스토리지 1Gi 정도 가정
    	            }            		
            	}
	            
	            // ========================= limits ==============================
	            Map<String, Long> lmap = psInfo.getNewLimitsMap();
            	
	            obj = lmap.get(PodWorker.POD_CPU);
	            if(obj != null) {
	            	temp = this.getFormmatCpu(obj);
	            	psLimits.setCpu(temp + "m");               // CPU 밀리코어 
            	}else {
            		if(wlLimits.getCpu() != 0) {
    		            temp = this.getFormmatCpu((long)wlLimits.getCpu());
    		            psLimits.setCpu(temp + "m");               // CPU 밀리코어
    	            }
            	}
	            
	            obj = lmap.get(PodWorker.POD_MEMORY);
	            if(obj != null) {
	            	temp = this.getNumberString(bytesToMi(obj));
	            	psLimits.setMemory(temp + "Mi");           // 메모리 MiB
            	}else {
            		if(wlLimits.getMemory() != 0) {
    		            temp = this.getNumberString(bytesToMi(wlLimits.getMemory()));
    		            psLimits.setMemory(temp + "Mi");           // 메모리 MIB
    	            }
            	}
	            
	            obj = lmap.get(PodWorker.POD_GPU);
	            if(obj != null) {
	            	temp = this.getNumberString(obj);
	            	psLimits.setGpu(temp);                 // GPU 1개
            	}else {
            		if(wlLimits.getGpu() != 0) {
    		            temp = this.getNumberString(wlLimits.getGpu());
    		            psLimits.setGpu(temp);                 // GPU 1
    	            }
            	}
	            
	            obj = lmap.get(PodWorker.POD_DISK);
	            if(obj != null) {
	            	temp = this.getNumberString(obj);
	            	psRequests.setEphemeralStorage(temp); // 스토리지
            	}else {
            		if(wlLimits.getEphemeralStorage() != 0) {
    	            	temp = this.getNumberString(wlLimits.getEphemeralStorage());
    	            	psLimits.setEphemeralStorage(temp); // 임시 스토리지 MIB
    	            }
            	}
	            
            }else {
            	if(wlRequests.getCpu() != 0) {
		            temp = this.getFormmatCpu((long)wlRequests.getCpu());
		            psRequests.setCpu(temp + "m");               // CPU 밀리코어 => core
            	}
	            
	            if(wlRequests.getMemory() != 0) {
		            temp = this.getNumberString(bytesToMi(wlRequests.getMemory()));
		            psRequests.setMemory(temp + "Mi");           // 메모리 MiB
	            }
	            
	            if(wlRequests.getGpu() != 0) {
		            temp = this.getNumberString(wlRequests.getGpu());
		            psRequests.setGpu(temp);                 // GPU 1개
	            }
	            
	            if(wlRequests.getEphemeralStorage() != 0) {
		            temp = this.getNumberString(wlRequests.getEphemeralStorage());
		            psRequests.setEphemeralStorage(temp); // 임시 스토리지 1Gi 정도 가정
	            }
	            
	            // ========================= limits ==============================
	            
	            if(wlLimits.getCpu() != 0) {
		            temp = this.getFormmatCpu((long)wlLimits.getCpu());
		            psLimits.setCpu(temp + "m");               // CPU 밀리코어 => core
	            }
	            
	            if(wlLimits.getMemory() != 0) {
		            temp = this.getNumberString(bytesToMi(wlLimits.getMemory()));
		            psLimits.setMemory(temp + "Mi");           // 메모리 MIB
	            }
	            
	            if(wlLimits.getGpu() != 0) {
		            temp = this.getNumberString(wlLimits.getGpu());
		            psLimits.setGpu(temp);                 // GPU 1
	            }
	            
	            if(wlLimits.getEphemeralStorage() != 0) {
	            	temp = this.getNumberString(wlLimits.getEphemeralStorage());
	            	psLimits.setEphemeralStorage(temp); // 임시 스토리지 MIB
	            }
            }
            
            // resources에 요청(requests), 제한(limits) 설정
            psRes.setRequests(psRequests);
            psRes.setLimits(psLimits);

            // container1에 resource 설정
            psC.setResources(psRes);

            // containers 리스트에 container1 추가
            psContainers.add(psC);        	
        }
        // 최종적으로 request 객체에 containers 설정
        psRequest.setContainers(psContainers);
      //}}기존 요청정보를 스케일링 요청정보의 입력으로 초기화 및 해당하는 파드 스케일링 처리함.
        
        // Jackson ObjectMapper를 이용해서 JSON 문자열로 변환(Pretty Print)
        String jsonString = null;
		try {
			jsonString = JSONUtil.getJsonstringFromObject(psRequest);
		} catch (JsonProcessingException e) {
			log.error("Pod Scaling 요청 에러:{}", psRequest);
			psRequest.clear();
			return;
		}
		
		try {
			String url = this.hybrid_request_url + APIConstant.REQUEST_SCALE;
			
			if(log.isDebugEnabled()) {
				log.info("Pod Scaling Request:{}\n{}", url, jsonString);
			}
			
			String responseString = "";
			
			try {			
				ResponseEntity<String> response = restTemplateUtil.post( url, jsonString, this.reDeployHeaders, String.class, ScalingInfoService.log);
				responseString = response.getBody();
			}catch(HttpServerErrorException  e) {
				log.error("Pod API call error:", e);
				responseString = e.getResponseBodyAsString();
			}
			
			if(log.isInfoEnabled()) {
				log.info("Pod Scaling Request:{}, response:{}", jsonString, responseString);
			}
			
			//요청 정보 저장
			ScalingInfo sinfo = new ScalingInfo();
			sinfo.setScalingType(StringConstant.SCALING_TYPE_POD);
			sinfo.setDocType(StringConstant.SCALING_DOC_TYPE_REQUEST);
			sinfo.setDocBody(jsonString);
			sinfo.setDocDesc(psInfo.toStringJson());
			sinfo.setDocResponse(responseString);
			dao.insertScalingInfo(sinfo);
			//
			
			//결과를 원본에 반영해야하나????????????????????????????
			//일단은 초기값을 또 추적해야 하므로 여기에 저장된 내용으로 확인하자.
			//이벤트 DB 등록
			comService.createEvent("파드 스케일링 요청", "PodScaleR"
					, "파드의 리소스를 조정을 위한 재배포 요청." 
					+ StringConstant.STR_lineFeed + "  워크로드: "     + promPod.getMlId() 
					+ StringConstant.STR_lineFeed + "  클러스터: "     + promPod.getClUid() 
					+ StringConstant.STR_lineFeed + ", 노드: "        + promPod.getNode() 
					+ StringConstant.STR_lineFeed + ", 대상 파드: "    + promPod.getPod()
					+ StringConstant.STR_lineFeed + ", 대상 컨테이너: " + mlContainerName
			);
		}catch(Exception e) {
			log.error("POD Scaling 요청 에러:{}", jsonString, e);
		}
		
		psRequest.clear();
    }
    
    /**
     * 클러스터 어드민 통합 포탈에 노드 스케일링 요청
     */
    public void requestNodeScalingApiCall(List<NodeScalingInfo> nodeScalingInfos) {
    	NodeScalingInfo nsInfo = nodeScalingInfos.get(0);
    	
    	String reason;
    	int nodeCount= nsInfo.total_node_count;
    	if(nsInfo.isHigh) {
    		reason = "노드 부족";
    		nodeCount++;
    		//nodeCount+=nodeScalingInfos.size(); //해당 갯수 만큼 
    	}else {
    		reason = "리소스 여뷰";
    		nodeCount--;
    		//nodeCount-=nodeScalingInfos.size(); //해당 갯수 만큼
    	}
    	
    	String nodeType = "normal";
    	if(nsInfo.gpu_isHigh != null) {
    		nodeType = "gpu";
    	}
    	
    	String callback_url = this.my_callback_url + APIConstant.MY_CALLBACK;
    	String url = this.api_base_url + APIConstant.API_CLUSTER_SCALE;
    	
    	NodeScalingInfoRequest nsiRequest = new NodeScalingInfoRequest();
    	nsiRequest.setClusterId(nsInfo.cur_node.getClUid());
    	nsiRequest.setNodeCount(nodeCount);
    	nsiRequest.setNodeType(nodeType); //optional: gpu, normal
    	nsiRequest.setCallbackUrl(callback_url);
    	nsiRequest.setReason(reason);
    	
    	String jsonString = null;
 		try {
	 		jsonString = JSONUtil.getJsonstringFromObject(nsiRequest);
	 	} catch (JsonProcessingException e) {
	 		log.error("Nodew Scaling 요청 에러:{}", nsiRequest);
	 		return;
	 	}
 		
 		String desc_json_string = null; //DB에 발생 원인을 저장하기 위함
 		try {
 			desc_json_string = JSONUtil.getJsonstringFromObject(nodeScalingInfos);
	 	} catch (JsonProcessingException e) {
	 		desc_json_string = null;
	 	}
	 		
	 	try {
			ResponseEntity<String> response = restTemplateUtil.get( url, jsonString, this.portalHeaders, String.class);
			String responseString = response.getBody();
			if(log.isInfoEnabled()) {
				log.info("Node Scaling Request:{}", jsonString, responseString);
			}
			
			//요청 정보 저장
			ScalingInfo sinfo = new ScalingInfo();
			sinfo.setScalingType(StringConstant.SCALING_TYPE_NODE);
			sinfo.setDocType(StringConstant.SCALING_DOC_TYPE_REQUEST);
			sinfo.setDocBody(jsonString);
			sinfo.setDocDesc(desc_json_string);
			sinfo.setDocResponse(responseString);
			dao.insertScalingInfo(sinfo);
			
			comService.createEvent("노드 스케일 요청", "NodeScaleR"
					, "클러스터의 여유 노드로 인한 스케일 요청.\n클러스터 아이디:" + nsiRequest.getClusterId() 
					+ ", 요청 정보:" + jsonString
					+ ", 원인 정보:" + nodeScalingInfos
				);
 		}catch(Exception e) {
 			log.error("Node Scaling 요청 에러:{}", jsonString, e);
 		}
    }

    
	public void processNodeScalingCallback(String msg) {
	//	Map<String, Object> msgMap = JSONUtil.getMapFromJsonString(msg);
	//	String code    = (String)msgMap.get("code");
	//	String message = (String)msgMap.get("message");
		
		//callback 수신 정보 저장
		ScalingInfo sinfo = new ScalingInfo();
		sinfo.setScalingType(StringConstant.SCALING_TYPE_NODE);
		sinfo.setDocType(StringConstant.SCALING_DOC_TYPE_RESPONSE);
		sinfo.setDocBody(msg);
		sinfo.setDocDesc("callback 수신");
		dao.insertScalingInfo(sinfo);
	}    
}

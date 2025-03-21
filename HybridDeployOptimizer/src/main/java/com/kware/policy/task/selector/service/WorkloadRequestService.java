package com.kware.policy.task.selector.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kware.common.util.JSONUtil;
import com.kware.policy.task.collector.service.vo.Cluster;
import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.WorkloadCommandManager;
import com.kware.policy.task.common.constant.APIConstant;
import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;
import com.kware.policy.task.common.queue.WorkloadContainerQueue;
import com.kware.policy.task.common.vo.WorkloadCommand;
import com.kware.policy.task.feature.FeatureMain;
import com.kware.policy.task.feature.service.vo.ClusterFeature;
import com.kware.policy.task.feature.service.vo.ClusterNodeFeature;
import com.kware.policy.task.feature.service.vo.WorkloadFeature;
import com.kware.policy.task.selector.service.algo.BestFitBinPackingV2;
import com.kware.policy.task.selector.service.config.ResourceWeightProperties;
import com.kware.policy.task.selector.service.config.ResourceWeightProperties.ResourceWeight;
import com.kware.policy.task.selector.service.dao.WorkloadRequestDao;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Request;
import com.kware.policy.task.selector.service.vo.WorkloadResponse;
import com.kware.policy.task.selector.service.vo.WorkloadResponse.Response;
import com.kware.policy.task.selector.service.vo.WorkloadResponseStatus;
import com.kware.policy.task.selector.service.vo.WorkloadTaskWrapper;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class WorkloadRequestService {
	public final static String interface_version = APIConstant.POLICY_INTERFACE_VERSION;
	
	@Autowired
	private ResourceWeightProperties rwProperties;
	
	@Autowired
	private FeatureMain featureMain;
	
	//{{db관련 서비스
	@Autowired
	protected WorkloadRequestDao dao;
	
	QueueManager qm = QueueManager.getInstance();	
	PromQueue    promQ         = qm.getPromQ();
	RequestQueue requestQ      = qm.getRequestQ();
	APIQueue     apiQ          = qm.getApiQ();
	
	WorkloadContainerQueue wcQ = qm.getWorkloadContainerQ();
	
	public int insertUserRequest(Request vo) {
		return dao.insertMoUserRequest(vo);
	}
	
	public int updateUserRequest_noti(Request vo) {
		return dao.updateMoUserRequest_noti(vo);
	}
	
	public int updateUserRequest_complete(String mlId) {
		return dao.updateMoUserRequest_complete(mlId);
	}

	public int insertMoUserResponse(Response vo) {
		return dao.insertMoUserResponse(vo);
	}
	
	@SuppressWarnings("rawtypes")
	@PostConstruct
	private void init_getDBWorkloadrequest() {
		List<Map> wlist = dao.selectOldWorkloadRequest();
		
		WorkloadRequest wlRequest = null;
//		WorkloadResponse wlResponse = null;
		for(Map map : wlist) {
			String reqString = (String)map.get("req");
			Integer clUid    = (Integer)map.get("cluid");
//			String resString = (String)map.get("res");
			try {
				wlRequest = JSONUtil.fromJsonToEmptyFromNull(reqString , WorkloadRequest.class);
//				wlResponse = JSONUtil.fromJsonToEmptyFromNull(resString, WorkloadResponse.class);
				if(log.isDebugEnabled())
					log.debug("load mlId: {}", wlRequest.getRequest().getMlId());
				
				wlRequest.getRequest().setClUid(clUid);
				this.setOldWorkloadRequest(wlRequest);
			} catch (Exception e) {
				log.error("Init DB WorkloadRequest Parser Error", e);
				continue;
			}
		}
		requestQ.setWrWervice(this);
	}
	//}}db 관련 서비스
	
	//{{노드 셀렉터
	public WorkloadResponse getResponseToSelectedNode(WorkloadRequest wlRequest) {
			
		List<PromMetricNode>  nodes = promQ.getLastPromMetricNodesReadOnly();
		Map<String, Cluster> culsters = apiQ.getApiClusterMap();
		Map<String, Map<String, WorkloadTaskWrapper>> notApplyRequestMap = wcQ.getNotCompletedTasks(); //wcQ.getNotRunningTasks();

		//{{Workload Type
	//	WorkloadRequest.WorkloadType workloadtype = wlRequest.getRequest().getAttribute().getWorkloadType();
		ResourceWeight resoruceWeight = null;
		//null이면 defaul 값을 리턴한다.
		//resoruceWeight = this.rwProperties.getResourceWeight(workloadtype);
		resoruceWeight = this.rwProperties.getResourceWeight(null);
		//}}
		
		WorkloadRequest.Request req = wlRequest.getRequest();
		req.setRequestDt(LocalDateTime.now());
		
		//{{응답  기본 생성
		WorkloadResponse wlResponse = new WorkloadResponse();
    	wlResponse.setVersion(WorkloadRequestService.interface_version);
    	
    	WorkloadResponse.Response res = new WorkloadResponse.Response();
    	res.setReqUid(req.getUid());
    	res.setMlId(req.getMlId());
    	//res.setName(req.getName());    	
    	res.setDate(new Date());
    	//}}응답  기본 생성
    	

    	boolean isOK = true;
    	
		//{{워크로드 특성반영을 통한 클러스터 및 노드 필터링 진행
		List<PromMetricNode> targetNodes = null;
		String priorityClass = null;
		String preemptionPolicy = null;
		StringBuffer strBuf = null;
		WorkloadFeature wlFeature = this.getWorkloadFeatureFromWorkloadRequest(wlRequest);
		
		
		boolean isUnmodifiable = false;
		if(wlFeature == null) {
			targetNodes = nodes;
			isUnmodifiable = true;
			//{{선점 비선점 정책 넣어볼까? checkpoint도 점검하고, 일단
			
			//}}
			
			//워크로드특성 로그 생성
			strBuf = getWorkloadFeatureLogging(null, null);
		}else {
			targetNodes = getNodeWithWorkloadFeatuerFilter(wlRequest, wlFeature, nodes);
			
			//워크로드특성 로그 생성
			strBuf = getWorkloadFeatureLogging(wlFeature, targetNodes);
			
			if(targetNodes.isEmpty()) {
				isOK = false;
			}else {
				priorityClass = wlFeature.getPriorityClass();
				preemptionPolicy = wlFeature.getPreemptionPolicy();
			}
		}
		//}}

		List<WorkloadTaskWrapper> wrapperList = null;
		WorkloadResponseStatus status = null;
		
		if(isOK) {
			//WorkloadTaskContainerWrapper 생성
			wrapperList = wcQ.makeWorkloadTaskContainerForInit(req.getAttribute(),req.getContainers());
			BestFitBinPackingV2 bbp = new BestFitBinPackingV2(targetNodes, notApplyRequestMap, resoruceWeight);
			bbp.setClusters(culsters);
			//실제 노드를 선택해하
			bbp.findOptimizerClusterNodes(wrapperList);
	    	
			for(WorkloadTaskWrapper w: wrapperList) {
				if(w.getNodeName() == null) {
					isOK = false;
					break;
				}
			}
			
			//노드 선택 이유를 저장
			res.setCause(strBuf.toString() + bbp.getCauseBuf().toString());			
		}
		
		if(isOK) {
			//배포전의 데이터는 배포완료가 되면 반영해야하나? 배포도 안했는데, 요청을 등록해야하나?
			//아니 노드를 찾으면 결과를 주고 모두 제거하고, 배포완료 명령이 오면 처리해야겠다.
			WorkloadCommand<List<WorkloadTaskWrapper>> command = new WorkloadCommand<List<WorkloadTaskWrapper>>(WorkloadCommand.CMD_WLD_ENTER, wrapperList);
			WorkloadCommandManager.addCommand(command);
	    	
	    	//List<Container> containers = req.getContainers();
			//List<PromMetricNode> sel_nodes = new ArrayList<PromMetricNode>();
			Integer clUid = wrapperList.get(0).getClUid();
			String strClUid = clUid.toString();
    		res.setClUid(strClUid);//clUid는 null일 수 없다.
    		for(WorkloadTaskWrapper w:  wrapperList) {
				res.addContainer(w.getName(), strClUid, w.getNodeName(), w.getEstimatedStartTime(),  priorityClass, preemptionPolicy);
	    	//}}
    		}
    		
	    	
    		//큐에 등록
    		wlRequest.getRequest().setClUid(clUid);
    		requestQ.setWorkloadRequest(wlRequest);
    		
	    	status = WorkloadResponseStatus.SUCCESS;
    	}else {
    		status = WorkloadResponseStatus.SUCCESS_NO_NODE;    		
    	}

    	res.setCode(status.getCode());
    	res.setMessage(status.getMessage());
    	//}}
    	   	
    	wlResponse.setResponse(res);
    	try {
    		res.setInfo(JSONUtil.getJsonstringFromObject(wlResponse));
		} catch (JsonProcessingException e) {
			log.error("결과 변환에러:{}", e, wlResponse);
		}
    	
    	if(targetNodes != null) {
    		if(!isUnmodifiable)
    			targetNodes.clear();
    	}

    	return wlResponse;
	}
	//}}
	
	/**
	 * 워크로드 특성을 반영한 노드 필터링 작업 진행
	 * @param wlRequest
	 * @param nodes
	 * @return
	 */
	private List<PromMetricNode> getNodeWithWorkloadFeatuerFilter(WorkloadRequest wlRequest, WorkloadFeature _wlFeature ,  List<PromMetricNode> nodes) {
		WorkloadFeature wlFeature = _wlFeature;
		Map<Integer, ClusterFeature> clFeatuerMap     = this.featureMain.getClusterFeature();
		Map<String, ClusterNodeFeature> clnFeatuerMap = this.featureMain.getClusterNodeFeature();
		
		List<PromMetricNode> resultNodeList = new ArrayList<PromMetricNode>();
		
		
		String cloudType = wlFeature.getCloudType();
		int nodeLevel = wlFeature.getNodeLevel();
		int gpuLevel  = wlFeature.getGpuLevel();  //값이 없으면 0으로 설정
		int secLevel  = wlFeature.getSecurityLevel();
		
		if(log.isDebugEnabled())
			log.debug("Request WorkloadFeatur:{}", wlFeature);
		
		Integer temp_nodeLevel, temp_gpuLevel, temp_secLevel;
		

		int temp_clUid;
		String temp_cloudType;
		boolean isOK = false;
		for(PromMetricNode node : nodes) {
			if(!node.canHandle())
				continue;
			
			isOK = false;
			temp_nodeLevel=temp_gpuLevel=temp_secLevel=null;
			temp_clUid = node.getClUid();

			ClusterFeature clFeature = clFeatuerMap.get(temp_clUid);
			ClusterNodeFeature clnFeature = clnFeatuerMap.get(node.getKey());
			
			if(clFeature != null && clnFeature != null) {
				temp_cloudType = clFeature.getCloudType();
				if(cloudType == null) isOK = true;
				else {
					isOK = temp_cloudType == null || cloudType.equals(temp_cloudType);
				}
			
				if(isOK) {
					temp_nodeLevel = clnFeature.getPerformanceLevel();
					isOK = temp_nodeLevel == null || temp_nodeLevel >= nodeLevel ;
				}
				if(isOK) {
					temp_gpuLevel  = clnFeature.getGpuLevel();
					isOK = temp_gpuLevel == null || temp_gpuLevel >= gpuLevel;
				}
				
				if(isOK) {
					temp_secLevel  = clnFeature.getSecurityLevel();
					isOK = temp_secLevel == null || temp_secLevel >= secLevel;
				}
				
				if(log.isDebugEnabled())
					log.debug("Compare Node Feature ##cloud:{}, nodeFeature:{}=> {}"
							, temp_cloudType, clnFeature, isOK );
			}
			
			if(isOK)
				resultNodeList.add(node);
		}
		return resultNodeList;
	}
	
	/**
	 * 요청한 워크로드 특성을 조회한다. 
	 * @param wlRequest
	 * @return
	 */
	private WorkloadFeature getWorkloadFeatureFromWorkloadRequest(WorkloadRequest wlRequest) {

		//this.featureMain;
		String wlFeatureIdStr = wlRequest.getRequest().getAttribute().getWorkloadFeature();
		if(wlFeatureIdStr == null)
			return null;

		int wlFeatureId = 0;
		try {
			wlFeatureId = Integer.parseInt(wlFeatureIdStr);
		}catch(NumberFormatException nfe) {
			log.error("특성ID 변환 에러:{}", wlFeatureIdStr, nfe);
		}
		 
		//워크로드 기본 특성은 front를 통해서 언제든지 변화할 수 있어서 자시 가져오야 한다.
		List<WorkloadFeature> wlFeatureList = this.featureMain.getFeatureBase_workloadFeature();
		
		WorkloadFeature wlFeature = null;
		for(WorkloadFeature slf : wlFeatureList) {
			if(wlFeatureId == slf.getId()){
				wlFeature = slf;
				break;
			}
		}
		return wlFeature;
	}

	//{{배포 통지
	/**
	 * 리퀘스트 배포 완료 통지 처리 
	 * @param wlRequest
	 * @param nodeConnt
	 * @return
	 */
	public void scheduleNotiRequest(WorkloadRequest wlRequest) {
		//요청 큐에서 찾아서 통지정보 갱신하고
		WorkloadRequest memoryWlRequest = requestQ.getWorkloadRequest(wlRequest.getRequest().getMlId());
		if(memoryWlRequest != null) {
			memoryWlRequest.getRequest().setNotiDt(LocalDateTime.now());
		}
	}
	//}}
	
	
	/**
	 * 프로그램 다시 시작할때 기존의 데이터를 다시 로딩하기 위함 종료안 된 워크로드 재등록
	 * 테스트 용도
	 * @param wlRequest
	 * @return
	 */
	public void setOldWorkloadRequest(WorkloadRequest wlRequest) {
		WorkloadRequest.Request req = wlRequest.getRequest();
		List<WorkloadTaskWrapper> wrapperList = wcQ.makeWorkloadTaskContainerForInit(req.getAttribute(),req.getContainers());
		
		WorkloadCommand<List<WorkloadTaskWrapper>> command = new WorkloadCommand<List<WorkloadTaskWrapper>>(WorkloadCommand.CMD_WLD_ENTER, wrapperList);
		WorkloadCommandManager.addCommand(command);
		
   		//큐에 등록
   		requestQ.setWorkloadRequest(wlRequest);
	}
	//}}
	
	/**
	 * 워크로드특성관련 로그를 생성하기 위함		
	 * @param _wlFeature  특성
	 * @param _targetNodes 필터링된 노드
	 * @return
	 */
	private StringBuffer getWorkloadFeatureLogging(WorkloadFeature _wlFeature, List<PromMetricNode> _targetNodes) {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append("Request Workload Feature Filter");
		strBuf.append(StringConstant.STR_lineFeed);
		strBuf.append(StringConstant.STR_tab1);
		if(_wlFeature == null) {
			strBuf.append("No workload feature information available.");
		}else {
			//{{원인 내용 생성
			strBuf.append(_wlFeature.toString2());
			strBuf.append(StringConstant.STR_lineFeed);
			strBuf.append(StringConstant.STR_tab1 + "Filtering Nodes:");
			strBuf.append(StringConstant.STR_lineFeed);
			strBuf.append(StringConstant.STR_tab2);
			for(PromMetricNode n : _targetNodes) {
				strBuf.append("[clUid:" + n.getClUid() + ", node:" + n.getNode() + "] ");
			}			
			//}}
		}
		strBuf.append(StringConstant.STR_lineFeed);
		
		return strBuf;
	}
	
	public int getWorkloadRequestContainerCount(String mlId) {
		return dao.selectWorkloadRequestContainerCount(mlId);
	}
	
}
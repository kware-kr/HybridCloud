package com.kware.policy.task.selector.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kware.common.util.JSONUtil;
import com.kware.policy.task.collector.service.vo.Cluster;
import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.WorkloadCommandManager;
import com.kware.policy.task.common.constant.APIConstant;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.APIQueue.APIMapsName;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;
import com.kware.policy.task.common.queue.WorkloadContainerQueue;
import com.kware.policy.task.common.vo.WorkloadCommand;
import com.kware.policy.task.selector.service.algo.BestFitBinPackingV2;
import com.kware.policy.task.selector.service.config.ResourceWeightProperties;
import com.kware.policy.task.selector.service.config.ResourceWeightProperties.ResourceWeight;
import com.kware.policy.task.selector.service.dao.WorkloadRequestDao;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Container;
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
	//}}db 관련 서비스
	
	//{{노드 셀렉터
	public WorkloadResponse getResponseToSelectedNode(WorkloadRequest wlRequest) {
			
		List<PromMetricNode>  nodes = promQ.getLastPromMetricNodesReadOnly();
		Map<String, Map<String, WorkloadTaskWrapper>> notApplyRequestMap = wcQ.getNotCompletedTasks(); //wcQ.getNotRunningTasks();
		Map<String, Cluster> culsters = apiQ.getApiClusterMap();

		//{{Workload Type
	//	WorkloadRequest.WorkloadType workloadtype = wlRequest.getRequest().getAttribute().getWorkloadType();
		ResourceWeight resoruceWeight = null;
		//null이면 defaul 값을 리턴한다.
		//resoruceWeight = this.rwProperties.getResourceWeight(workloadtype);
		resoruceWeight = this.rwProperties.getResourceWeight(null);
		//}}

		BestFitBinPackingV2 bbp = new BestFitBinPackingV2(nodes, notApplyRequestMap, resoruceWeight);
		bbp.setClusters(culsters);
		
		WorkloadRequest.Request req = wlRequest.getRequest();
		req.setRequestDt(LocalDateTime.now());

		//WorkloadTaskContainerWrapper 생성
		List<WorkloadTaskWrapper> wrapperList = wcQ.makeWorkloadTaskContainerForInit(req.getAttribute(),req.getContainers());
		bbp.setClusterNodes(wrapperList);
		
		WorkloadResponse wlResponse = new WorkloadResponse();
    	wlResponse.setVersion(WorkloadRequestService.interface_version);
    	
    	//{{응답 생성
    	WorkloadResponse.Response res = new WorkloadResponse.Response();
    	res.setReqUid(req.getUid());
    	res.setMlId(req.getMlId());
    	//res.setName(req.getName());    	
    	res.setDate(new Date());
    	
    	WorkloadResponseStatus status = null;
		boolean isOK = true;
		for(WorkloadTaskWrapper w: wrapperList) {
			if(w.getNodeName() == null) {
				isOK = false;
				break;
			}
		}
		
		if(isOK) {
			//배포전의 데이터는 배포완료가 되면 반영해야하나? 배포도 안했는데, 요청을 등록해야하나?
			//아니 노드를 찾으면 결과를 주고 모두 제거하고, 배포완료 명령이 오면 처리해야겠다.
			WorkloadCommand<List<WorkloadTaskWrapper>> command = new WorkloadCommand<List<WorkloadTaskWrapper>>(WorkloadCommand.CMD_WLD_ENTER, wrapperList);
			WorkloadCommandManager.addCommand(command);
			
	    	
	    	List<Container> containers = req.getContainers();
			//List<PromMetricNode> sel_nodes = new ArrayList<PromMetricNode>();
			Integer clUid = wrapperList.get(0).getClUid();
			String strClUid = clUid.toString();
    		res.setClUid(strClUid);//clUid는 null일 수 없다.
    		for(WorkloadTaskWrapper w:  wrapperList) {
				res.addContainer(w.getName(), strClUid, w.getNodeName(), w.getEstimatedStartTime(),  null, null);
	    	//}}
    		}
	    	
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

    	return wlResponse;
	}
	//}}
	
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
	
}
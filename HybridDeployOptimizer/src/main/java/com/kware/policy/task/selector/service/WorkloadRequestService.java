package com.kware.policy.task.selector.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kware.common.util.JSONUtil;
import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.constant.APIConstant;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;
import com.kware.policy.task.selector.service.algo.BestFitBinPacking;
import com.kware.policy.task.selector.service.config.ResourceWeightProperties;
import com.kware.policy.task.selector.service.config.ResourceWeightProperties.ResourceWeight;
import com.kware.policy.task.selector.service.dao.WorkloadRequestDao;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Container;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Request;
import com.kware.policy.task.selector.service.vo.WorkloadResponse;
import com.kware.policy.task.selector.service.vo.WorkloadResponse.Response;
import com.kware.policy.task.selector.service.vo.WorkloadResponseStatus;

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
	PromQueue    promQ = qm.getPromQ();
	RequestQueue requestQ = qm.getRequestQ();

	public int insertMoUserRequest(Request vo) {
		return dao.insertMoUserRequest(vo);
	}

	public int insertMoUserResponse(Response vo) {
		return dao.insertMoUserResponse(vo);
	}
	//}}db 관련 서비스
	
	
	/**
	 * 
	 * @param wlRequest
	 */
	public void setCompleteNoti(WorkloadRequest wlRequest) {
		
	}
	
	
	//{{노드 셀렉터
	public WorkloadResponse getResponseToSelectedNode(WorkloadRequest wlRequest) {	
		List<PromMetricNode>  nodes = promQ.getLastPromMetricNodesReadOnly();
		Map<String, Map<String, Container>>  notApplyRequestMap = requestQ.getWorkloadRequestNotApplyReadOnlyMap();
		
		
		wlRequest.aggregate(true);

		//{{Workload Type
	//	WorkloadRequest.WorkloadType workloadtype = wlRequest.getRequest().getAttribute().getWorkloadType();
		ResourceWeight resoruceWeight = null;
		//null이면 defaul 값을 리턴한다.
		//resoruceWeight = this.rwProperties.getResourceWeight(workloadtype);
		resoruceWeight = this.rwProperties.getResourceWeight(null);
		//}}

		BestFitBinPacking bbp = new BestFitBinPacking(nodes, notApplyRequestMap, resoruceWeight);
		
		WorkloadRequest.Request req = wlRequest.getRequest();

		List<Container> containers = req.getContainers();
		List<PromMetricNode> sel_nodes = new ArrayList<PromMetricNode>();
		Integer clUid = null;
		for(int i = 0 ; i < containers.size(); i++) {
			Container container =  containers.get(i);
			
			List<PromMetricNode>  tempNodes = bbp.allocate(container, clUid);
			log.info("select node list: {}", tempNodes);

			PromMetricNode node = tempNodes.size() > 0 ? tempNodes.get(0):null;
			tempNodes.clear();
			
			if(node != null) {
				clUid = node.getClUid();//한개의 워크로드는 동일한 클러스터에서 수행한다.
				//workloadrequest에 컨테이너의 순서대로 선택된 노드명을 등록함
				wlRequest.getNodes().add(node.getNode());
				wlRequest.setClUid(clUid);
				
				
				//노드가 선택되면 notApplyRequestMap에 추가해 주어야 한다.
				requestQ.setWorkloadRequest(wlRequest, i);
			}
			
			sel_nodes.add(node); //null이어도 등록한다.
		}
		
		
		
		
		
		
		
		
		
		
		
		//배포전의 데이터는 배포완료가 되면 반영해야하나? 배포도 안했는데, 요청을 등록해야하나?
		//아니 노드를 찾으면 결과를 주고 모두 제거하고, 배포완료 명령이 오면 처리해야겠다.
	
		
		
		
		
		
		
		
		
		
		
		
		
		
		


		WorkloadResponse wlResponse = new WorkloadResponse();
    	wlResponse.setVersion(WorkloadRequestService.interface_version);
    	
    	//{{응답 생성
    	WorkloadResponse.Response res = new WorkloadResponse.Response();
    	
    	res.setReqUid(req.getUid());
    	res.setMlId(req.getMlId());
    	//res.setName(req.getName());    	
    	res.setDate(new Date());
    	
    	//rsResult.setClusterName("Workload-cluster-01");
    	WorkloadResponseStatus status = null;
    	boolean isOK = true;
    	if(sel_nodes != null) {	
    		res.setClUid(clUid.toString());//clUid는 null일 수 없다.
    		for(int i = 0 ; i < containers.size(); i++) {
    			Container container =  containers.get(i);
    			PromMetricNode node = sel_nodes.get(i);
	    	/*
	    	//{{이게 for loop 돌아야 한다.
	    	WorkloadResponse.Response.ContainerResult crResult = new WorkloadResponse.Response.ContainerResult();
	    	
	    	crResult.setNode(node.getNode());
	    	//crResult.setNoUuid(node.getNoUuid());

	    	//{{이건 어떻게 해야하나
	    	crResult.setPreemptionPolicy(null);
	    	crResult.setPriorityClass(null);
	    	//}}
	    	*/
    			//한개라도 null이 있으면 어떻게 하나?????
    			if(node != null) {
    				res.addContainers(container.getName(), res.getClUid(), node.getNode(), null, null);
    			}else {
    				isOK = false;
    			}
	    	//}}
    		}
    	}
    	
    	//1개라도 선택하지 못하면
    	if(isOK) {
	    	status = WorkloadResponseStatus.SUCCESS;
    	}else {
    		status = WorkloadResponseStatus.SUCCESS_NO_NODE;    		
    	}

    	res.setCode(status.getCode());
    	res.setMessage(status.getMessage());
    	//}}
    	   	
    	//res.setResult(rsResult);
    	wlResponse.setResponse(res);
    	
    	//qm.
    	
    	try {
    		res.setInfo(JSONUtil.getJsonstringFromObject(wlResponse));
		} catch (JsonProcessingException e) {
			log.error("결과 변환에러:{}", e, wlResponse);
		}

    	return wlResponse;
	}
	//}}
	
	
	//{{노드 셀렉터 테스트
	/**
	 * 요청 리퀘스트에 대한 노드의 스코어를 요청한 갯수 만큼 리턴 
	 * @param wlRequest
	 * @param nodeConnt
	 * @return
	 */
	public List<PromMetricNode> getNodeScore(WorkloadRequest wlRequest, int nodeCount) {
		List<PromMetricNode>  nodes = promQ.getLastPromMetricNodesReadOnly();
		Map<String,Map<String, Container>>  notApplyRequestMap = requestQ.getWorkloadRequestNotApplyReadOnlyMap();
		

		//WorkloadRequest.WorkloadType workloadtype = wlRequest.getRequest().getAttribute().getWorkloadType();
		ResourceWeight resoruceWeight = null;
		//null이면 defaul 값을 리턴한다.
		resoruceWeight = this.rwProperties.getResourceWeight(null);

		BestFitBinPacking bbp = new BestFitBinPacking(nodes, notApplyRequestMap, resoruceWeight);

		WorkloadRequest.Request req = wlRequest.getRequest();

		List<Container> containers = req.getContainers();
		List<PromMetricNode> sel_nodes = new ArrayList<PromMetricNode>();
		Integer clUid = null;
		for(int i = 0 ; i < containers.size(); i++) {
			Container container =  containers.get(i);
			
			List<PromMetricNode>  tempNodes = bbp.allocate(container, clUid);
			log.info("select node list: {}", sel_nodes);

			PromMetricNode node = tempNodes.size() > 0 ? tempNodes.get(0):null;
			tempNodes.clear();
			
			if(node != null) {
				clUid = node.getClUid();//한개의 워크로드는 동일한 클러스터에서 수행한다.
				//workloadrequest에 컨테이너의 순서대로 선택된 노드명을 등록함
				wlRequest.getNodes().add(node.getNode());
				wlRequest.setClUid(clUid);
				
				//노드가 선택되면 notApplyRequestMap에 추가해 주어야 한다.
				requestQ.setWorkloadRequest(wlRequest, i);
			}
			sel_nodes.add(node);
		}
		
		//테스트 이므로 모두 삭제한다.
		requestQ.reomveWorkloadRequest(wlRequest.getRequest().getMlId());

		return sel_nodes;
	}
	//}}
	
	//{{배포완료 통지
	/**
	 * 리퀘스트 배포 완료 통지 처리 
	 * @param wlRequest
	 * @param nodeConnt
	 * @return
	 */
	public void completeRequest(WorkloadRequest wlRequest) {
		WorkloadRequest memoryWlRequest = requestQ.getWorkloadRequestMap().get(wlRequest.getRequest().getMlId());
		memoryWlRequest.setComplete_notice_militime(System.currentTimeMillis()); //통지시간 설정
	}
	//}}
}
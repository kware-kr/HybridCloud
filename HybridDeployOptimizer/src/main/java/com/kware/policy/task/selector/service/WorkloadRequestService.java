package com.kware.policy.task.selector.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	
	//{{노드 셀렉터
	public WorkloadResponse getNodesSelector(WorkloadRequest wlRequest) {
		List<PromMetricNode>  nodes = promQ.getLastPromMetricNodesReadOnly();
		Map<String, Set<WorkloadRequest>>  notApplyRequestMap = requestQ.getWorkloadRequestNotApplyReadOnlyMap();
		

		WorkloadRequest.WorkloadType workloadtype = wlRequest.getRequest().getRequestAttributes().getWorkloadType();
		ResourceWeight resoruceWeight = null;
		//null이면 defaul 값을 리턴한다.
		resoruceWeight = this.rwProperties.getResourceWeight(workloadtype);

		BestFitBinPacking bbp = new BestFitBinPacking(nodes, notApplyRequestMap, resoruceWeight);

		List<PromMetricNode>  sel_nodes = bbp.allocate(wlRequest);
		log.info("select node list: {}", sel_nodes);

		PromMetricNode node = sel_nodes.size() > 0 ? sel_nodes.get(0):null;
		sel_nodes.clear();

		WorkloadRequest.Request req = wlRequest.getRequest();

		WorkloadResponse wlResponse = new WorkloadResponse();
    	wlResponse.setVersion(WorkloadRequestService.interface_version);
    	
    	//{{응답 생성
    	WorkloadResponse.Response res = new WorkloadResponse.Response();
    	res.setUid(req.getUid());
    	res.setId(req.getId());
    	//res.setName(req.getName());    	
    	res.setDate(new Date());
    	
    	WorkloadResponse.Response.ResponseResult rsResult = new WorkloadResponse.Response.ResponseResult();
    	//rsResult.setClusterName("Workload-cluster-01");
    	WorkloadResponseStatus status = null;
    	if(node != null) {
	    	rsResult.setClusterId(node.getClUid().toString());
	    	rsResult.setNodeName(node.getNode());
	    	rsResult.setNodeId(node.getNoUid());

	    	//{{이건 어떻게 해야하나
	    	rsResult.setPreemptionPolicy(null);
	    	rsResult.setPriority(null);
	    	//}}

	    	status = WorkloadResponseStatus.SUCCESS;
    	}else {
    		status = WorkloadResponseStatus.SUCCESS_NO_NODE;    		
    	}

    	res.setCode(status.getCode());
    	res.setMessage(status.getMessage());
    	//}}
    	res.setResult(rsResult);
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
	
	//{{노드 셀렉터
	/**
	 * 요청 리퀘스트에 대한 노드의 스코어를 요청한 갯수 만큼 리턴 
	 * @param wlRequest
	 * @param nodeConnt
	 * @return
	 */
	public List<PromMetricNode> getNodeScore(WorkloadRequest wlRequest, int nodeConnt) {
		List<PromMetricNode>  nodes = promQ.getLastPromMetricNodesReadOnly();
		Map<String, Set<WorkloadRequest>>  notApplyRequestMap = requestQ.getWorkloadRequestNotApplyReadOnlyMap();
		

		WorkloadRequest.WorkloadType workloadtype = wlRequest.getRequest().getRequestAttributes().getWorkloadType();
		ResourceWeight resoruceWeight = null;
		//null이면 defaul 값을 리턴한다.
		resoruceWeight = this.rwProperties.getResourceWeight(workloadtype);

		BestFitBinPacking bbp = new BestFitBinPacking(nodes, notApplyRequestMap, resoruceWeight);

		List<PromMetricNode>  sel_nodes = bbp.allocate(wlRequest, nodeConnt); //

		return sel_nodes;
	}
	//}}
}
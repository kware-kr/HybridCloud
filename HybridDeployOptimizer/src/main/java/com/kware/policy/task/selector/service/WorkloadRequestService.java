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
		//Map<String, Map<String, Container>>  notApplyRequestMap = requestQ.getWorkloadRequestNotApplyReadOnlyMap();
		//Map<String, Map<String, Container>>  notApplyRequestMap = new HashMap(); //임시
		Map<String, Map<String, WorkloadTaskWrapper>> notApplyRequestMap = wcQ.getNotCompletedTasks(); //wcQ.getNotRunningTasks();
		Map<String, Cluster> culsters = apiQ.getApiClusterMap();
		
		
//		wlRequest.aggregate(true);

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
		
	/*
		List<PromMetricNode> sel_cluster_nodes = bbp.getBestCluster(wrapperList);
		if(sel_cluster_nodes == null || sel_cluster_nodes.size() == 0) {
			log.error("선택 된 node size 0");
		}else {
			clUid = sel_cluster_nodes.get(0).getClUid();
			for(int i = 0 ; i < wrapperList.size(); i++) {
				WorkloadTaskWrapper w =  wrapperList.get(i);
				//clUid가 null이면 현재 클러스터설정을 첫번째 컨테이너로만 계산 하는데, 수정필요하겠다, 
				//전체 값으로, 그리고 순서를 가진 것중에서는 앞의 선서와 다른 경우에는 바로 앞의 것은 포함하지 않도록
				List<PromMetricNode>  tempNodes = null;// bbp.allocate(w, 2, clUid);  
				log.info("select node list: {}", tempNodes);
	
				//첫번째 노드를 선택함
				PromMetricNode node = tempNodes.size() > 0 ? tempNodes.get(0):null;
				tempNodes.clear();
				
				if(node != null) {
					clUid = node.getClUid();//한개의 워크로드는 동일한 클러스터에서 수행한다.
					wlRequest.getRequest().setClUid(clUid);
					
					w.setNodeName(node.getNode());
					w.setClUid(clUid);
					
					//노드가 선택되면 notApplyRequestMap에 추가해 주어야 한다.
					requestQ.setWorkloadRequest(wlRequest);
					
					
					
					
				}
				
				sel_nodes.add(node); //null이어도 등록한다.
			}
			sel_cluster_nodes.clear();
		}
*/
		
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

				res.addContainer(w.getName(), strClUid, w.getNodeName(), w.getEstimatedStartTime(),  null, null);
	    	//}}
    		}
	    	
	    	
    	//1개라도 선택하지 못하면
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
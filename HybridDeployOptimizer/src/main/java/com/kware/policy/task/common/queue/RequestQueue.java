package com.kware.policy.task.common.queue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역으로 사용할 스레드에 안전한 큐와 맵을 생성함

not
요청에 응답했지만 prometheus metric에 발견되지 않을때 얼마동안 가지고 있다가 없엘지 확인
 */
@Slf4j
public class RequestQueue {
	private static final Logger queueLog = LoggerFactory.getLogger("queue-log");
    
	//{{ISSUE
	//requestMap은 파드가 종료되고 없어지면 제거해야 겠네.
	//}}ISSUE
	
    //{{모든 요청된 request를 관리하고, metric에 나타나면 requestNotAppliMap과 함께 삭제
    //요청한 request 관리: key: WorkloadRequest.request.id ==> mlUid
    private final ConcurrentHashMap<String, WorkloadRequest> requestMap;
    
    //응답을 했지만 실제 적용이 안된 request: cl_uid + "_" + node_name(메트릭에는 노드 uid가 없다)
    private final ConcurrentHashMap<String, Set<WorkloadRequest>> requestNotApplyMap;
    //}}요청관리
  
    public RequestQueue() {
    	queueLog.info("Queue Log Start ====================================================="); //로그 파일 생성하는 목적
    	log.error("Error Log Start ====================================================="); //로그 파일 생성하는 목적
        requestMap = new ConcurrentHashMap<String, WorkloadRequest>();
        requestNotApplyMap = new ConcurrentHashMap<String, Set<WorkloadRequest>>();
    }
    
    //---------------------------------------------------------------------------------------------------
  	/**
  	 * 요청한 WorkloadRequest class를 관리하는 ConcurrentHashMap
  	 * @return Map<String, WorkloadRequest>
  	 */
  	public Map<String, WorkloadRequest> getWorkloadRequestMap() {
  		return (ConcurrentHashMap<String, WorkloadRequest>)requestMap;
  	}
  	
  	/**
     * 워크로드 요청 전체.
     * @return ReadOnly Map<String,WorkloadRequest>
     */
    public Map<String,WorkloadRequest> getWorkloadRequestReadOlnyMap() {
    	return (ConcurrentHashMap<String, WorkloadRequest>) Collections.unmodifiableMap(this.requestMap);    	
    }
  	
  	/**
  	 * 요청한 WorkloadRequest class 중에서 현재 배포되었지만 Metric에 
  	 * 나타나지 않는 과도기 상태의 요청를 관리하는 ConcurrentHashMap
  	 * @return Map<String, Set<WorkloadRequest>>
  	 */
  	public Map<String, Set<WorkloadRequest>> getWorkloadRequestNotApplyMap() {
  		return (ConcurrentHashMap<String, Set<WorkloadRequest>>)requestNotApplyMap;
  	}
  	
    /**
     * 노드에 배포요청이 완료되었지만, 실제 서버에 배포되지않는 읽기전용 Map을 제공한다.
     * @return ReadOnly Map<String, Set<WorkloadRequest>>
     */
    public Map<String,Set<WorkloadRequest>> getWorkloadRequestNotApplyReadOnlyMap() {
    	return Collections.unmodifiableMap(this.requestNotApplyMap);    	
    }	
  	
    /**
     * requestMap(요청) 사이즈
     * @return int
     */
  	public int getWorkloadRequestMapSize() {
  		return this.requestMap.size();
  	}
  	
  	/**
  	 * requestNotApplyMap size
  	 * @return
  	 */
  	public int getWorkloadRequestNotApplyMapSize() {
  		return this.requestNotApplyMap.size();
  	}
    
    //{{ ///////////////////////// RequestWorkloadNode /////////////////////////////////

    //각 검색하기 편하게 두개의 키로 동일한 자료를 등록한다. id, clusterId + "_" + nodename
    public void  setWorkloadRequest(WorkloadRequest _req) {
    	if(_req.getClUid() == null || _req.getNode() == null || _req.getRequest().getId() == null) {
    		throw new NullPointerException ("clUid, node, request.id is nullable");
    	}
       	
    	//동일한 키가 있으면 값을 현재 버전으로 수정하고 이전 버전을 리턴한다.(내용물이 틀릴 수 있으므로)
    	this.requestMap.put(_req.getRequest().getId(),_req);
    	
    	String key = _req.getNodeKey();
    	Set<WorkloadRequest> list = this.requestNotApplyMap.get(key);
    	if(list == null) {
    		//list = new CopyOnWriteArrayList<WorkloadRequest>();
    		list = ConcurrentHashMap.newKeySet();
    		this.requestNotApplyMap.put(key, list);
    	}
    	//Set은 Object의 일부를 키로 할때 (hashcode, equals 함수를 통함) 값이 다르지만 키가 같아서 수정하지 않고 false를 리턴한다,
    	// 그래서 여기서는 제거하고 다시 등록하도록 한다.
    	if(list.contains(_req)) {
    		list.remove(_req);
    	}
    	list.add(_req);
    }
    
    /**
     * workload mlId를 통해서 요청정보를 조회
     * @param _mlUid
     * @return
     */
    public WorkloadRequest getWorkloadRequest(String _mlUid) {
    	return this.requestMap.get(_mlUid);
    }
    
    
    
    /**
     * 여기는 실제 작업이 종료되면 삭제되어야하고, 메트릭에 나타나지 않으면 삭제되어야 한다.
     * 좀 복잡하지만 api에서도 삭제되어 한다.
     * requestMap에 등록된 객체 인스턴스와 notApplayRequestNodeMap에 있는 객체는 동일하다. 
     * 하지만 requestMap에는 배포가 결정된 것과, 노드가 결정되지 않은 WorkloadRequest가 함께 존재한다.
     * 
     * @param _ml_uid
     */
    public void  reomveWorkloadRequest(String _mlUid) {
    	WorkloadRequest req = this.requestMap.get(_mlUid);
    	if(req != null) {
    		reomveNotApplyWorkloadRequest(req);
    		this.requestMap.remove(req.getNodeKey());
    	}
    }
    
    /**
     * Metric에 나타나면 배포된 것으로 보고, notapplyMapt에서 삭제
     * @param WrokloadRequest 
     */
    public void  reomveNotApplyWorkloadRequest(WorkloadRequest _req) {
    	if(_req == null) 
    		return;
    	
    	String key = _req.getNodeKey();
    	Set<WorkloadRequest> list = this.requestNotApplyMap.get(key);
    	if(list != null ) {
    		list.remove(_req);
    	}
    }
    
    
    
    /**
     * 해당 노드에 배포요청이 완료되었지만, 실제 서버에 배포되지않는 워크로드 Set을 제공한다.
     * @param _clUid 클러스터 아이디
     * @param _node  노드 명
     * @return
     */
    public Set<WorkloadRequest> getNotApplyWorkloadRequestSetForNode(Integer _clUid, String _node) {
    	Set<WorkloadRequest> list = this.requestNotApplyMap.get(_clUid + StringConstant.STR_UNDERBAR + _node);
    	if(list == null) return null;
    	
    	//readonly로 변경해서 보낸다.
    	return Collections.unmodifiableSet(list); 
    }
    /**
     * 해당 노드에 배포요청이 완료되었지만, 실제 서버에 배포되지않는 워크로드 List을 제공한다.
     * @param Integer _clUid 클러스터 아이디
     * @param String _node  노드 명
     * @return
     */
    public List<WorkloadRequest> getNotApplyWorkloadRequestListForNode(Integer _clUid, String _node) {
    	Set<WorkloadRequest> set = this.requestNotApplyMap.get(_clUid + StringConstant.STR_UNDERBAR + _node);
    	
    	if(set == null) return null;
    	
    	List<WorkloadRequest> list = new ArrayList<>(set);
    	
    	return list;
    }
}
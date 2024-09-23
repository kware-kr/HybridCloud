package com.kware.policy.task.common.queue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Container;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역으로 사용할 스레드에 안전한 큐와 맵을 생성함

not
요청에 응답했지만 prometheus metric에 발견되지 않을때 얼마동안 가지고 있다가 없엘지 확인
 */
@Slf4j
public class RequestQueue  extends DefaultQueue{
	private static final Logger queueLog = LoggerFactory.getLogger("queue-log");
    
	//{{ISSUE
	//requestMap은 파드가 종료되고 없어지면 제거해야 겠네.
	//}}ISSUE
	
    //{{모든 요청된 request를 관리하고, metric에 나타나면 requestNotAppliMap과 함께 삭제
    //요청한 request 관리: key: WorkloadRequest.request.id ==> mlUid
    private final ConcurrentHashMap<String, WorkloadRequest> requestMap;
    
    //응답을 했지만 실제 적용이 안된 request: cl_uid + "_" + node_name(메트릭에는 노드 uid가 없다);즉 노드에 할당된 워크로드의 컨테이너(파드)
    //내부 Map의 키: mlid+'_" + container_name
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Container>> requestNotApplyMap;
    //}}요청관리
  
    public RequestQueue() {
    	queueLog.info("Queue Log Start ====================================================="); //로그 파일 생성하는 목적
    	log.error("Error Log Start ====================================================="); //로그 파일 생성하는 목적
        requestMap = new ConcurrentHashMap<String, WorkloadRequest>();
        requestNotApplyMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, Container>>();
    }
    
    @Override
    public void setScheduler(TaskScheduler scheduler) {
        this.scheduler = scheduler;
        //스케줄러 생성
        this.createCleanSchedulerForRequestQueue();//requestMap clean
    }  
    
    //---------------------------------------------------------------------------------------------------
  	/**
  	 * 요청한 WorkloadRequest class를 관리하는 ConcurrentHashMap
  	 * @return Map<String, WorkloadRequest>
  	 */
  	public Map<String, WorkloadRequest> getWorkloadRequestMap() {
  		return this.requestMap;
  	}
  	
  	/**
     * 워크로드 요청 전체.
     * @return ReadOnly Map<String,WorkloadRequest>
     */
    public Map<String,WorkloadRequest> getWorkloadRequestReadOlnyMap() {
    	return Collections.unmodifiableMap(this.requestMap);    	
    }
  	
  	/**
  	 * 요청한 WorkloadRequest class 중에서 현재 배포되었지만 Metric에 
  	 * 나타나지 않는 과도기 상태의 요청를 관리하는 ConcurrentHashMap
  	 * @return Map<String, Set<WorkloadRequest>>
  	 */
  	public Map<String, ConcurrentHashMap<String, Container>> getWorkloadRequestNotApplyMap() {
  		return this.requestNotApplyMap;
  	}
  	
    /**
     * 노드에 배포요청이 완료되었지만, 실제 서버에 배포되지않는 읽기전용 Map을 제공한다.
     * @return ReadOnly Map<String, Set<WorkloadRequest>>
     */
    public Map<String, Map<String,Container>> getWorkloadRequestNotApplyReadOnlyMap() {
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

  	/**
  	 * 노드선택 알고리즘을 적용한 후에 워크로드 등록하고 및 워크로드에 포함된 모든 컨테이너를 배포전의 노드관리에 등록한다.
  	 * @param _req
  	 */
    //각 검색하기 편하게 두개의 키로 동일한 자료를 등록한다. id, clusterId + "_" + nodename
    public void  setWorkloadRequest(WorkloadRequest _req) {
    	String mlId = _req.getRequest().getRequestKey();
    	if(_req.getClUid() == null || _req.getNodes() == null || mlId == null) {
    		throw new NullPointerException ("clUid, node, request.id is nullable");
    	}
    	
    	long timemilis = System.currentTimeMillis(); //등록시간을 저장해서 나중에 expierd time을 적용하기 위함
    	_req.setTimemillisecond(timemilis);
       	
    	//동일한 키가 있으면 값을 현재 버전으로 수정하고 이전 버전을 리턴한다.(내용물이 틀릴 수 있으므로)
    	this.requestMap.put(mlId, _req);
    	
    	//특정 클러스터의 노드에 등록된 모든 워크로드를 관리
    	int containerSize = _req.getRequest().getContainers().size();
    	for(int i=0; i < containerSize; i++) {
	    	String nodeKey = _req.getNodeKey(i);
	    	
	    	ConcurrentHashMap<String, Container> containerMap = this.requestNotApplyMap.get(nodeKey);
	    	if(containerMap == null) {
	    		//list = new CopyOnWriteArrayList<WorkloadRequest>();
	    		// 스레드에 안전한(즉, 여러 스레드에서 동시에 접근해도 안전한) 키 집합을 생성
	    		containerMap = new ConcurrentHashMap<String, Container>();
	    		this.requestNotApplyMap.put(nodeKey, containerMap);
	    	}
	    	//Set은 Object의 일부를 키로 할때 (hashcode, equals 함수를 통함) 값이 다르지만 키가 같아서 수정하지 않고 false를 리턴한다,
	    	// 그래서 여기서는 제거하고 다시 등록하도록 한다.
	    	
	    	Container container = _req.getRequest().getContainers().get(i);
	    	container.setTimemillisecond(timemilis);
	    	String containerKey = container.getContainerKey(mlId); 
	    	
	    	containerMap.put(containerKey, container);
    	}
    }
    
    /**
  	 * 노드선택 알고리즘이 컨테이너에 적용될때 해당하는 컨테이너만 배포전의 노드관리에 등록한다.
  	 * @param _req
  	 */
    //각 검색하기 편하게 두개의 키로 동일한 자료를 등록한다. id, clusterId + "_" + nodename
    public void  setWorkloadRequest(WorkloadRequest _req, Integer containerIdx) {
    	String mlId = _req.getRequest().getRequestKey();
    	if(_req.getClUid() == null || _req.getNodes() == null || mlId == null) {
    		throw new NullPointerException ("clUid, node, request.id is nullable");
    	}
    	
    	long timemilis = System.currentTimeMillis(); //등록시간을 저장해서 나중에 expierd time을 적용하기 위함
    	if(_req.getTimemillisecond() == 0L)
    		_req.setTimemillisecond(timemilis);
       	
    	//동일한 키가 있으면 값을 현재 버전으로 수정하고 이전 버전을 리턴한다.(내용물이 틀릴 수 있으므로)
    	this.requestMap.put(mlId, _req);
    	
    	//특정 클러스터의 노드에 등록된 모든 워크로드를 관리
    	
	    	String nodeKey = _req.getNodeKey(containerIdx);
	    	
	    	ConcurrentHashMap<String, Container> containerMap = this.requestNotApplyMap.get(nodeKey);
	    	if(containerMap == null) {
	    		//list = new CopyOnWriteArrayList<WorkloadRequest>();
	    		// 스레드에 안전한(즉, 여러 스레드에서 동시에 접근해도 안전한) 키 집합을 생성
	    		containerMap = new ConcurrentHashMap<String, Container>();
	    		this.requestNotApplyMap.put(nodeKey, containerMap);
	    	}
	    	//Set은 Object의 일부를 키로 할때 (hashcode, equals 함수를 통함) 값이 다르지만 키가 같아서 수정하지 않고 false를 리턴한다,
	    	// 그래서 여기서는 제거하고 다시 등록하도록 한다.
	    	
	    	Container container = _req.getRequest().getContainers().get(containerIdx);
	    	container.setTimemillisecond(timemilis);
	    	
	    	String containerKey = container.getContainerKey(mlId); //mlId + "_" + container name
	    	containerMap.put(containerKey, container);
    	
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
    		reomveWorkloadRequest(req);
    	}
    }
    
    public void  reomveWorkloadRequest(WorkloadRequest _req) {
   		reomveNotApplyWorkloadRequest(_req);
   		this.requestMap.remove(_req.getRequest().getMlId());
    }
    
    /**
     * Metric에 나타나면 배포된 것으로 보고, notapplyMapt에서 삭제
     * @param WrokloadRequest 
     */
    private void  reomveNotApplyWorkloadRequest(WorkloadRequest _req) {
    	if(_req == null) 
    		return;
    	
    	String mlId = _req.getRequest().getRequestKey();
    	
    	//노드에 배포할려고하는 모든 워크로드 리스트를 관리하는 HashMap를 가지고 와서, 그 안에 있는 것 중에서 나의 워크로드에 속한 요청을 가지고 온다. 
    	int containerSize = _req.getRequest().getContainers().size();
    	for(int i=0; i < containerSize; i++) {
	    	String nodeKey = _req.getNodeKey(i);
	    	Map<String, Container> containerMap = this.requestNotApplyMap.get(nodeKey);
	    	if(containerMap != null ) {
	    		Container container = _req.getRequest().getContainers().get(i);
		    	String containerKey = container.getContainerKey(mlId); 
	    		containerMap.remove(containerKey);
	    	}
    	}
    }
    
    /**
     * 해당 노드에 배포요청이 완료되었지만, 실제 서버에 배포되지않는 워크로드 Container을 제공한다.
     * @param _clUid 클러스터 아이디
     * @param _node  노드 명
     * @return
     */
    public Map<String, Container> getNotApplyWorkloadRequestSetForNode(Integer _clUid, String _node) {
    	Map<String, Container> containerMap = this.requestNotApplyMap.get(_clUid + StringConstant.STR_UNDERBAR + _node);
    	if(containerMap == null) return null;
    	
    	//readonly로 변경해서 보낸다.
    	return Collections.unmodifiableMap(containerMap); 
    }
    /**
     * 해당 노드에 배포요청이 완료되었지만, 실제 서버에 배포되지않는 워크로드 List을 제공한다.
     * @param Integer _clUid 클러스터 아이디
     * @param String _node  노드 명
     * @return
     */
    public List<Container> getNotApplyWorkloadRequestListForNode(Integer _clUid, String _node) {
    	Map<String, Container> map = this.requestNotApplyMap.get(_clUid + StringConstant.STR_UNDERBAR + _node);
    	
    	if(map == null) return null;
    	
    	List<Container> list = new ArrayList<>(map.values());
    	
    	return list;
    }
    
    //_miliseconds가 지난 데이터를 삭제하는 루틴
    public boolean isExpiredElements(String _mlId, long _expried_militime) {
    	
    	WorkloadRequest element = this.requestMap.get(_mlId);
    	
    	long cutoffTime = System.currentTimeMillis() - _expried_militime;
    	long noti_complete_time = element.getComplete_notice_militime();
    	long create_time = element.getTimemillisecond();
    	
    	if(noti_complete_time == 0L) {
    		if(create_time < cutoffTime) {
    	        if(queueLog.isDebugEnabled()) {
    	        	queueLog.debug("RequestMap-{}: remove mlId: {}, 나머지 갯수:{}", element.getRequest().getMlId(), requestMap.size() );
    	        	queueLog.debug("**********************************************************************************");
    	        }
    	        return true;
    		}
    	}
    	return false;
    }
    
    long expired_time = 10 * 60 * 1000;//10분 
    //내부 스케줄링을 통해 제거작업등을 수행하도록 함
    //1.시간지난 promDeques 정리작업 진행
	private void createCleanSchedulerForRequestQueue() {
	
		// 5분마다 수행할 작업 정의
		Runnable periodicTask = new Runnable() {
			@Override
			public void run() {
				Iterator<String> iterator = requestMap.keySet().iterator();
				while (iterator.hasNext()) { // 큐 내부의 모든 요소를 순회하면서 제거
					String mlId = iterator.next();
					if(isExpiredElements(mlId, expired_time)) {
						reomveNotApplyWorkloadRequest(requestMap.get(mlId));
						iterator.remove();
					}
				}
			}
		};
		
		// 초기 지연 시간 없이 5분마다 작업을 실행
		scheduler.scheduleAtFixedRate(periodicTask, Instant.now(), Duration.ofMinutes(5)); // 처음 실행 시간을 약간 지연시킬 수 있음
	             // 5분 간격 (밀리초));
	}
}
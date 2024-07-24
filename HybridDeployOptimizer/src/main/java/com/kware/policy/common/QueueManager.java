package com.kware.policy.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kware.policy.service.vo.ClusterDefault;
import com.kware.policy.service.vo.PromMetricDefault;
import com.kware.policy.service.vo.PromMetricNode;
import com.kware.policy.service.vo.PromMetricNodes;
import com.kware.policy.service.vo.PromMetricPod;
import com.kware.policy.service.vo.PromMetricPods;
import com.kware.policy.task.StringConstant;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역으로 사용할 스레드에 안전한 큐와 맵을 생성함
 * @param <T>
 */
@Slf4j
public class QueueManager {
	private static final Logger queueLog = LoggerFactory.getLogger("queue-log");
	
	
    private static final QueueManager instance = new QueueManager();
 
    //{{API
    public static enum APIMapsName{	CLUSTER, NODE, WORKLOAD, WORKLOADPOD 	};
    //keyinfo=> cluster: cluid, node: cluid + "_" + name, workload: mlid, workloadpod: pod uid
    private final Map<APIMapsName, ConcurrentHashMap<String, ?>> apiMaps;//API결과 저장, 최신 정보 1개만 저장, (현재는 지우고 저장하지 않음. 수정필요)
    //}}
    
    //{{Prometheus
    //수집한 메트릭을 기반으로 처리함: nodeinfo는 스케줄링을 위한, pod는 스케일링을 위한 용도 사용
    public static enum PromDequeName{   	METRIC_NODEINFO, METRIC_PODINFO    };
    //PromDequeName별도 다른 Object를 BlockingDeque에 관리한다.,PromMetricNodes, PromMetricPods
    private final Map<PromDequeName, BlockingDeque<?>> promDeques; //프로메테우스 metric 저장, Node저장 deque, pod저장 deque
    //}}Prometheus
    
    //{{요청관리
    //요청한 request 관리: key: WorkloadRequest.request.id==> mluid
    private final ConcurrentHashMap<String, WorkloadRequest> requestMap;
    //실제 적용이 안된 정보관리: cl_uid + "_" + node_name(메트릭에는 노드 uid가 없다)
    private final ConcurrentHashMap<String, Set<WorkloadRequest>> notApplyRequestNodeMap;
    //}}요청관리
    
    
  //collect는 단지 수집된 정보를 저장하는 용도,
  	//proc는 구조화된 정보를 저장하고, 최신 데이터만 저장
  	/*
    public static enum QueueName{
  		METRIC_COLLECT, CL_COLLECT, METRIC_PROC, CL_PROC
  	};
  	*/

    private QueueManager() {
    	queueLog.info("Queue Log Start ====================================================="); //로그 파일 생성하는 목적
    	log.error("Error Log Start ====================================================="); //로그 파일 생성하는 목적
        /*
    	queues      = new ConcurrentHashMap<>();
        api_maps    = new ConcurrentHashMap<>();
        prom_deques = new ConcurrentHashMap<>();
        */
        apiMaps    = new HashMap<>();
        promDeques = new HashMap<>();
        requestMap = new ConcurrentHashMap<String, WorkloadRequest>();
        
        notApplyRequestNodeMap = new ConcurrentHashMap<String, Set<WorkloadRequest>>();
        
        //스케줄러 생성
        checkingScheduleForManager();
    }

    public static QueueManager getInstance() {
        return instance;        
    }
  
    
    //{{ //////////////deque LIFO, FIFO모두 사용가능 ==> LIFO로 사용하자. //////////////////////
    
    public BlockingDeque<?> getPromDeque(PromDequeName name) {
        return promDeques.computeIfAbsent(name, k -> new LinkedBlockingDeque<>());
    }
    
    @SuppressWarnings({ "rawtypes"})
    public int getPromDequesSize(PromDequeName name) {
    	BlockingDeque deque = promDeques.get(name);
    	if(deque == null) 
    		return 0;
    		
    	return deque.size();
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public void addPromDequesObject(PromDequeName name, Object obj) {
    	BlockingDeque deque = promDeques.get(name);
    	if(deque == null) {
    		deque = getPromDeque(name);
    	}
    	deque.addFirst(obj);
    }
    
    public Object getPromDequesFirstObject(PromDequeName name) {
    	BlockingDeque<?> deque = promDeques.get(name);
    	if(deque == null)
    		return null;
    	else return deque.peekFirst();
    }
    
    public void clearePromDeques(PromDequeName name) {
    	BlockingDeque<?> q = promDeques.remove(name);
    	if(q == null) return;
    	
        Iterator<?> iterator = q.iterator();
        while (iterator.hasNext()) { // 큐 내부의 모든 요소를 순회하면서 제거
            PromMetricDefault element = (PromMetricDefault)iterator.next();
            element.clear();
            iterator.remove();
        }        
    }

    /**
     *  모든 큐를 제거하는 메서드: 
     *  여기는 실제 종료시점이므로 불필요함
     */
    public void removeAllDeque() {
    	Iterator<PromDequeName> iterator = promDeques.keySet().iterator();
        while (iterator.hasNext()) { // 큐 내부의 모든 요소를 순회하면서 제거
        	clearePromDeques(iterator.next());
        }
    }
    
    //_miliseconds가 지난 데이터를 삭제하는 루틴
    public void removeExpiredDequeElements(PromDequeName name, long _miliseconds) {
    	
    	long cutoffTime = System.currentTimeMillis() - _miliseconds;
    	BlockingDeque<?> q = promDeques.get(name);
    	
    	if(queueLog.isDebugEnabled()) {
    		queueLog.debug("**********************************************************************************");
    		queueLog.debug("Deque-{}: remove before: size: {}",name,  q.size());

    		Object obj = q.peekFirst();
    		if(obj instanceof PromMetricNodes) {
    			PromMetricNodes nodeList = (PromMetricNodes)obj;
    			for(PromMetricNode n: nodeList.getAllNodeList()) {
    				queueLog.debug("========================");
    				queueLog.debug("PromMetricNode: {}", n);
    				queueLog.debug("========================");
    			}
    		}else if(obj instanceof PromMetricPods) {
    			PromMetricPods podList = (PromMetricPods)obj;
    			for(PromMetricPod n: podList.getAllPodList()) {
    				queueLog.debug("++++++++++++++++++++++++");
    				queueLog.debug("PromMetricPod: {}", n);
    				
    				n.getMContainerList().forEach((key, value) -> {
    					queueLog.debug("---------------------------");
    					queueLog.debug("PromMetricContainer: {}", value);
    					queueLog.debug("---------------------------");
    				});
    				queueLog.debug("++++++++++++++++++++++++");
    			}
    		}
    	}
    	    			
    	Iterator<?> iterator = q.descendingIterator();
        
        while (iterator.hasNext()) {
        	PromMetricDefault element = (PromMetricDefault)iterator.next();
            if (element.getTimestamp() < cutoffTime) {
            	element.clear();
                iterator.remove();
            } else {
                break; // 타임스탬프가 더 최근인 경우, 더 이상 뒤로 갈 필요 없음
            }
        }
        
        if(queueLog.isDebugEnabled()) {
        	queueLog.debug("Deque-{}: remove after: size: {}",name, q.size());
        	queueLog.debug("**********************************************************************************");
        }
    }
    
    /**
     * 최신 MetricNode를 제공
     * @param name
     * @return List<PromMetricNode>
     */
    
    public List<PromMetricNode> getLastPromMetricNodesReadOnly() {
    	BlockingDeque<PromMetricNodes> nodeDeque = (BlockingDeque<PromMetricNodes>)getPromDeque(QueueManager.PromDequeName.METRIC_NODEINFO);
    	PromMetricNodes  nodes= nodeDeque.peekFirst();
    	
    	return nodes.getUnmodifiableAllNodeList();
    }
    
    /**
     * 배포가능한 노드 리스트 제공
     * @return List<PromMetricNode>
     */
    public List<PromMetricNode> getAppliablePromMetricNodesReadOnly() {
    	BlockingDeque<PromMetricNodes> nodeDeque = (BlockingDeque<PromMetricNodes>)getPromDeque(QueueManager.PromDequeName.METRIC_NODEINFO);
    	PromMetricNodes  nodes= nodeDeque.peekFirst();
    	return nodes.getUnmodifiableAppliableNodeList();
    }
    
    public List<PromMetricNode> getAppliablePromMetricNodesReadOnly(WorkloadRequest req) {
    	BlockingDeque<PromMetricNodes> nodeDeque = (BlockingDeque<PromMetricNodes>)getPromDeque(QueueManager.PromDequeName.METRIC_NODEINFO);
    	PromMetricNodes  nodes= nodeDeque.peekFirst();
    	
    	return nodes.getUnmodifiableAppliableNodeList(req.getTotalLimitCpu(), req.getTotalLimitMemory(), req.getTotalLimitDisk(), req.getTotalLimitGpu());
    }
    
    
    /**
     * Prometheus에서 수집된 다양한 최신 데이터를 조회(METRIC_PODINFO)
     * @return List<PromMetricPod>
     */
    public List<PromMetricPod> getLastPromMetricPodsReadOnly() {
    	BlockingDeque<PromMetricPods> deque = (BlockingDeque<PromMetricPods>)promDeques.get(QueueManager.PromDequeName.METRIC_PODINFO);
    	PromMetricPods pods = deque.peekFirst();
    	
    	return pods.getUnmodifiableAllPodList();
    }
    
    //}}
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    //{{ ///////////////////////// API /////////////////////////////////
    // ConcurrentHashMap 관련 메서드
    public ConcurrentHashMap<String, ?> getApiMap(APIMapsName name) {
        return apiMaps.computeIfAbsent(name, k -> new ConcurrentHashMap<>());
    }

 /*
    public Object getMapValue(mapsname mapName, String key) {
        ConcurrentHashMap<String, Object> map = api_maps.get(mapName);
        if (map != null) {
            return map.get(key);
        }
        return null;
    }

    public void putMapValue(mapsname mapName, String key, Object value) {
        ConcurrentHashMap<String, Object> map = api_maps.computeIfAbsent(mapName, k -> new ConcurrentHashMap<>());
        map.put(key, value);
    }

    public Object removeMapValue(mapsname mapName, String key) {
        ConcurrentHashMap<String, Object> map = api_maps.get(mapName);
        if (map != null) {
            return map.remove(key);
        }
        return null;
    }
*/
    /**
     * API에서 가장 최근 수집된 클러스터 HashMap을 리턴 
     * @return Map
     */
    public Map getApiLastClusters() {
    
    	Map map = apiMaps.get(APIMapsName.CLUSTER);
    	return map;
    }
    
    /**
     * API에서 가장 최근 수집된 클러스터 노드들 HashMap을 리턴
     * @return Map
     */
    public Map getApiLastClusterNodes() {
    	Map map = apiMaps.get(APIMapsName.NODE);
    	return map;
    }
    
    /**
     * API에서 가장 최근 수집된 Workloads HashMap 리턴
     * @return
     */
    public Map getApiLastWorkloads() {
    	Map map = apiMaps.get(APIMapsName.WORKLOAD);
    	return map;
    }
    
    /**
     * API에서 가장 최근 수집된 Workloads Pods HashMap 리턴
     * @return Map
     */
    public Map getApiLastWorkloadPods() {
    	Map map = apiMaps.get(APIMapsName.WORKLOADPOD);
    	return map;
    }
    
    public void clearApiMaps(APIMapsName name) {
        ConcurrentHashMap<String, ?> map = apiMaps.remove(name);
        
        String key = null;
        Iterator<String> iterator = map.keySet().iterator();
        while (iterator.hasNext()) { // 큐 내부의 모든 요소를 순회하면서 제거
        	key = iterator.next();
        	
        	ClusterDefault value = (ClusterDefault)map.get(key);
        	value.clear();
        	
            iterator.remove();
        }        
    }

    public void removeAllMaps() {
    	Iterator<APIMapsName> iterator = apiMaps.keySet().iterator();
        while (iterator.hasNext()) { // 큐 내부의 모든 요소를 순회하면서 제거
        	clearApiMaps(iterator.next());
        }
    }
    
    //API입력중에서 현재세션에서 제공한 데이터가 아닌 데이터를 즉 이전 세션에서 생성된 데이터를 제거함
    public void removeNotIfSessionId(APIMapsName name, String sessionId) {
    	ConcurrentHashMap<String, ?> map = apiMaps.get(name);
    	
    	String key = null;
    	Iterator<String> iterator =  map.keySet().iterator();
    	while (iterator.hasNext()) {
    	    key = iterator.next();
    	    ClusterDefault value = (ClusterDefault)map.get(key);
    	    
    	    String temp = null;
    	    
    	    if(value instanceof ClusterDefault) {
    	    	temp =value.getSessionId();
    	    }
/*    	    
    	    if(value instanceof Cluster ) {
    	    	temp = ((Cluster)value).getSessionId();
    	    }else if(value instanceof ClusterNode ) {
    	    	temp = ((ClusterNode)value).getSessionId();
    	    }else if(value instanceof ClusterWorkload ) {
    	    	temp = ((ClusterWorkload)value).getSessionId();
    	    }else {
    	    	;
    	    }
    	    */
    	    // 여기에 조건을 넣어서 조건이 맞으면 제거합니다.
    	    if (!sessionId.equals(temp)) {
    	    	value.clear();
    	        iterator.remove(); // 현재 엔트리를 안전하게 제거합니다.
    	    }
    	}
    }
    //}}API END
    
    
    
    
    
    
    
    
    
    
    //{{ ///////////////////////// RequestWorkloadNode /////////////////////////////////
    // ConcurrentHashMap 관련 메서드
    //public ConcurrentHashMap<String, WorkloadRequest> getWorkloadRequestMapMap() {
    //    return this.requestMap;
    //}
    //각 검색하기 편하게 두개의 키로 동일한 자료를 등록한다. id, clusterId + "_" + nodename
    public void  setWorkloadRequest(WorkloadRequest _req) {
    	if(_req.getClUid() == null || _req.getNode() == null || _req.getRequest().getId() == null) {
    		throw new NullPointerException ("clUid, node, request.id is nullable");
    	}
       	
    	//동일한 키가 있으면 값을 현재 버전으로 수정하고 이전 버전을 리턴한다.(내용물이 틀릴 수 있으므로)
    	this.requestMap.put(_req.getRequest().getId(),_req);
    	
    	String key = _req.getNodeKey();
    	Set<WorkloadRequest> list = this.notApplyRequestNodeMap.get(key);
    	if(list == null) {
    		//list = new CopyOnWriteArrayList<WorkloadRequest>();
    		list = ConcurrentHashMap.newKeySet();
    		this.notApplyRequestNodeMap.put(key, list);
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
     * 워크로드 요청 전체.
     * @return 수정불가능한 ConcurrentHashMap<String,WorkloadRequest>
     */
    public ConcurrentHashMap<String,WorkloadRequest> getWorkloadRequestMap() {
    	return (ConcurrentHashMap<String, WorkloadRequest>) Collections.unmodifiableMap(this.requestMap);    	
    }
    
    /**
     * 여기는 실제 메트릭에서 배포가 완료되면 삭제되어야함.
     * 노드를 선택하는 과정이 이 정보가 합산되어서 처리가 된다.
     * 참고: WorkloadRequest 등록 및 삭제는 동시에 이루어진단, mlId를 통해서는 requestMap에서 WorkloadReques를 검색할 수 있고
     * 그 값을 이용해서 notApplayRequestNodeMap을 조회, 삭제가 가능함 
     * 즉 requestMap에 등록된 객체 인스턴스가 notApplayRequestNodeMap에 있는 객체가 동일하다. 하지만 requestMap에는 배포가 결정된 것과, 
     * 노드가 결정되지 않은 WorkloadRequest가 함께 존재한다.
     * @param _ml_uid
     */
    public void  reomveWorkloadRequest(String _mlUid) {
    	WorkloadRequest req = this.requestMap.get(_mlUid);
    	if(req != null)
    		reomveWorkloadRequest(req);
    }
    
    public void  reomveWorkloadRequest(WorkloadRequest _req) {
    	if(_req == null) 
    		return;
    	
    	String key = _req.getNodeKey();
    	Set<WorkloadRequest> list = this.notApplyRequestNodeMap.get(key);
    	if(list != null ) {
    		list.remove(_req);
    	}
    	
    	this.requestMap.remove(key);
    }
    
    
    
    /**
     * 노드에 배포요청이 완료되었지만, 실제 서버에 배포되지않는 워크로드 Set을 제공한다.
     * @param _clUid
     * @param _node
     * @return
     */
    public Set<WorkloadRequest> getNotApplyWorkloadRequestSetForNode(Integer _clUid, String _node) {
    	Set<WorkloadRequest> list = this.notApplyRequestNodeMap.get(_clUid + StringConstant.STR_UNDERBAR + _node);
    	if(list == null) return null;
    	
    	//readonly로 변경해서 보낸다.
    	return Collections.unmodifiableSet(list); 
    }
    /**
     * 노드에 배포요청이 완료되었지만, 실제 서버에 배포되지않는 워크로드 List을 제공한다.
     * @param _clUid
     * @param _node
     * @return
     */
    public List<WorkloadRequest> getNotApplyWorkloadRequestListForNode(Integer _clUid, String _node) {
    	Set<WorkloadRequest> set = this.notApplyRequestNodeMap.get(_clUid + StringConstant.STR_UNDERBAR + _node);
    	
    	if(set == null) return null;
    	
    	List<WorkloadRequest> list = new ArrayList<>(set);
    	
    	return list;
    }
    
    /**
     * 노드에 배포요청이 완료되었지만, 실제 서버에 배포되지않는 워크로드 List을 제공한다.
     * @return 수정불가능한 ConcurrentHashMap<String,Set<WorkloadRequest>>
     */
    public Map<String,Set<WorkloadRequest>> getNotApplyWorkloadRequestForNode() {
    	return Collections.unmodifiableMap(this.notApplyRequestNodeMap);    	
    }
    
   
    
    
    
    /**
     *  여기는 실제 종료시점이므로 불필요함
     */
    public void clearesWorkloadRequestMap(PromDequeName name) {
    	this.notApplyRequestNodeMap.forEach((k,v)->{ v.clear(); 	});
    	this.notApplyRequestNodeMap.clear();
    	this.requestMap.clear();
    }
    
    //}}
    
    long expired_time = 30 * 60 * 1000;
    //내부 스케줄링을 통해 제거작업등을 수행하도록 함
    //1.시간지난 promDeques 정리작업 진행
	private void checkingScheduleForManager() {
		// ScheduledExecutorService 생성
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		// 10분마다 수행할 작업 정의
		Runnable periodicTask = new Runnable() {
			@Override
			public void run() {
				Iterator<PromDequeName> iterator = promDeques.keySet().iterator();
				while (iterator.hasNext()) { // 큐 내부의 모든 요소를 순회하면서 제거
					removeExpiredDequeElements(iterator.next(), expired_time);
				}
			}
		};

		// 초기 지연 시간 없이 5분마다 작업을 실행
		scheduler.scheduleAtFixedRate(periodicTask, 0, 5, TimeUnit.MINUTES);

		// 프로그램 종료 후 스케줄러 종료
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				scheduler.shutdownNow();
			}
		}));
	}
	
	
	
}
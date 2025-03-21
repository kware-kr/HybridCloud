package com.kware.policy.task.common.queue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kware.common.util.JSONUtil;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.common.constant.StringConstant.PodStatusPhase;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Container;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.RequestWorkloadAttributes;
import com.kware.policy.task.selector.service.vo.WorkloadTaskWrapper;

/**
 * 전역으로 사용할 스레드에 안전한 큐와 맵을 생성함

not
요청에 응답했지만 prometheus metric에 발견되지 않을때 얼마동안 가지고 있다가 없엘지 확인
 */
//@Slf4j
public class WorkloadContainerQueue  extends DefaultQueue{
	private static final Logger queueLog = LoggerFactory.getLogger("queue-log");
    
    private final ConcurrentHashMap<String/*cl_uid_node_name*/,	ConcurrentHashMap<String/*mlid_container-name*/, WorkloadTaskWrapper>> nodeMap;
    private final ConcurrentHashMap<String/*mlid*/, List<WorkloadTaskWrapper>> mlIdMap;
    //}}요청관리
  
    public WorkloadContainerQueue() {
    	this.nodeMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, WorkloadTaskWrapper>>();
    	this.mlIdMap = new ConcurrentHashMap<String/*mlId*/, List<WorkloadTaskWrapper>>();
    }
    
    /**
     * 단순한 WorkloadTaskContainer 생성하고, Queue에는 등록하지 않음
     * @param RequestWorkloadAttributes _reqattr
     * @param List<Container> _containerList
     */
    public List<WorkloadTaskWrapper> makeWorkloadTaskContainerForInit(RequestWorkloadAttributes _reqattr, List<Container> _containerList) {
    	//order 순으로 정열
    	_containerList.sort(Comparator.comparingInt(c-> c.getAttribute().getOrder()));
    	
    	WorkloadTaskWrapper wrapper = null;
    	List<WorkloadTaskWrapper> wrapperList = new ArrayList<WorkloadTaskWrapper>();
    	String mlId = null;
    	int idx = 0;
    	for(Container c: _containerList) {
    		c.setNameIdx(idx++); //키값 설정함
    		
    		wrapper = new WorkloadTaskWrapper(_reqattr, c);
    		wrapper.setNameIdx(c.getNameIdx());
    		if(mlId == null)
    			mlId = c.getMlId();
    		wrapperList.add(wrapper); 
    	}
    	this.startPod(wrapperList, null);
    	
    	return wrapperList;
    }

    
    /**
     * Queue에 등록 
     * @param RequestWorkloadAttributes _reqattr
     * @param List<Container> _containerList
     */
    public void setWorkloadTaskWrappers(List<WorkloadTaskWrapper> _wrapperList) {
    	if(_wrapperList == null) {
    		return;
    	}
    	String mlId = _wrapperList.get(0).getMlId();
    	
    	List<WorkloadTaskWrapper> wrapperList = this.mlIdMap.get(mlId);
    	if(wrapperList != null) {
    		wrapperList.clear(); //기존것 삭제
    	}
    	this.mlIdMap.put(mlId, _wrapperList);
    	
    	for(WorkloadTaskWrapper w: _wrapperList) {
    		this.setWorkloadTaskWrapper(w);
    	}
    	
    	this.startPod(_wrapperList, null);
    	
    	return;
   }
    
    
    private void setWorkloadTaskWrapper(WorkloadTaskWrapper _wrapper) {
    	String nodeKey      = _wrapper.getNodeKey();
    	String containerKey = _wrapper.getContainerKey();
    	
    	ConcurrentHashMap<String/*mlid_container-name*/, WorkloadTaskWrapper> cmap = this.nodeMap.get(nodeKey);
    	if(cmap == null) {
    		cmap = new ConcurrentHashMap<String, WorkloadTaskWrapper>();
    		this.nodeMap.put(nodeKey, cmap); //생성될때 한번만 입력하자, 변경은 값(객체) 변경은 자동 
    	}
    	
    	cmap.put(containerKey, _wrapper);
	}

	/**
     * Queue에 등록 
     * @param RequestWorkloadAttributes _reqattr
     * @param List<Container> _containerList
     */
    public void setWorkloadTaskContainers(RequestWorkloadAttributes _reqattr, List<Container> _containerList) {
    	if(_containerList == null) {
    		return;
    	}
    	//order 순으로 정열
    	_containerList.sort(Comparator.comparingInt(c-> c.getAttribute().getOrder()));
    	String mlId = _containerList.get(0).getMlId();
    	
    	List<WorkloadTaskWrapper> wrapperList = this.mlIdMap.get(mlId);
    	if(wrapperList == null) {
    		wrapperList = new ArrayList<WorkloadTaskWrapper>();
    		this.mlIdMap.put(mlId, wrapperList);
    	}
    	
    	WorkloadTaskWrapper wrapper = null;
    	for(Container w: _containerList) {
    		wrapper = this.setWorkloadTaskContainer(_reqattr, w);
    		wrapperList.add(wrapper);
    	}
    	
    	this.startPod(wrapperList, null);
    	
    	return;
   }
    
    //항상 그룹으로 등록하게 하고 개별적으로 등록하지 못하도록 private로 설정 한다.
   private WorkloadTaskWrapper setWorkloadTaskContainer(RequestWorkloadAttributes _reqattr, Container _container) {
    	String nodeKey      = _container.getNodeKey();
    	String containerKey = _container.getContainerKey();
//    	String mlidKey      = _container.getMlId();
    	
    	ConcurrentHashMap<String/*mlid_container-name*/, WorkloadTaskWrapper> cmap = this.nodeMap.get(nodeKey);
    	if(cmap == null) {
    		cmap = new ConcurrentHashMap<String, WorkloadTaskWrapper>();
    		this.nodeMap.put(nodeKey, cmap); //생성될때 한번만 입력하자, 변경은 값(객체) 변경은 자동 
    	}
    	
    	//wrapper가 등록되어 있으면 등록하지 않는다.
    	if(cmap.containsKey(containerKey)) {
    		return null;
    	}
    	
    	WorkloadTaskWrapper wrapper = new WorkloadTaskWrapper(_reqattr, _container);
    	cmap.put(containerKey, wrapper);
    	
    	return wrapper;
		/*
		List<WorkloadTaskContainerWrapper> wrapperList = this.mlIdMap.get(mlidKey);
		if(wrapperList == null) {
			wrapperList = new ArrayList<WorkloadTaskContainerWrapper>();
			this.mlIdMap.put(mlidKey, wrapperList);
		}
		wrapperList.add(wrapper);*/
    	
    }
        
   /**
    * Queue에서 제거함
    * @param mlId
    */
    public void removeWorkloadTaskContainer(String mlId) {
    	String nodeKey;
    	String containerKey;
    	String mlidKey = mlId;
    	
    	List<WorkloadTaskWrapper> wrapperList = this.mlIdMap.remove(mlidKey);
    	if(wrapperList != null) {
    		for(WorkloadTaskWrapper w: wrapperList) {
    			nodeKey = w.getNodeKey();
    			containerKey = w.getContainerKey();
    			this.nodeMap.get(nodeKey).remove(containerKey);
    		}
    		wrapperList.clear();
    	}
    }
   
    
    /**
     * 해당 컨테이너가 배포통지 또는 프로메테우스 쿼리를 통해 시작을 받을때 이후의 작업에 대한 부분을 모두 변경한다.
     * @param t
     * @param containerKey
     */
    private void startPod(List<WorkloadTaskWrapper> _wrapperList, WorkloadTaskWrapper _podWrapper) {
    	//int order     = -1;
    	int baseOrder = -1;
    	List<WorkloadTaskWrapper> wrapperList = _wrapperList;
    	
    	if(_podWrapper != null) {
    		//latestPodWrapper = getWrapperForLatestPod(wrapperList, _podUid);
    		baseOrder = _podWrapper.getOrder();  //기준 baseorder
    	}
    		
    	//현재 order를 포함해서 order 이후 리스트 그룹으로 동일한 order가 있어서 리스트로 표현
    	Map<Integer, List<WorkloadTaskWrapper>> orderGroupMap = filterAndGroupByOrder(wrapperList, baseOrder, true);
    	
    	
 	    LocalDateTime startTime = null;
 	    LocalDateTime endTime = null;
 	    LocalDateTime maxEndTime = null;  //동일한 order일 경우 최대값을 기준으로 다음 실행시간을 결정필요
 	    LocalDateTime preStartTime = null;
 	   
 	    /*
 	    if(_podWrapper == null) { //초기값
			LocalDateTime now = LocalDateTime.now();
			startTime = now.plusMinutes(1); // 최초 1분 후에 배포된다고 가정 
		}
		*/
 	    
    	for (Map.Entry<Integer, List<WorkloadTaskWrapper>> entry : orderGroupMap.entrySet()) {
    	    //order = entry.getKey();
    	    List<WorkloadTaskWrapper> sameOrderWrappers = entry.getValue();

    	    maxEndTime = null;
    	    for (WorkloadTaskWrapper w : sameOrderWrappers) {
     	        if(w.getScheduledTimestamp() != null) { //이미 실행되었으면 실행데이터 기준으로 하고
    	        	startTime = w.getScheduledTimestamp();
    	        }else {
    	        	startTime = w.getEstimatedStartTime();
    	        	if(startTime == null)
    	        		startTime = preStartTime;
    	        }
     	        
     	        if(startTime == null) {
     	        	LocalDateTime now = LocalDateTime.now();
     				startTime = now.plusMinutes(1); // 최초 1분 후에 배포된다고 가정
     	        }

    	        // 예상 실행시간 또는 실제 실행시간 을 기준으로 종료 시간 계산
    	        endTime = startTime.plusMinutes(w.getPredictedExecutionTime());
    	        
    	        if(maxEndTime == null)
    	        	maxEndTime = endTime;
    	        else if(maxEndTime.isAfter(endTime)) {
    	        	maxEndTime = endTime;
    	        }

    	        //예상 시작 시간과 종료 시간 설정
    	        if(w.getScheduledTimestamp() == null ) //시작하지 않는 경우만 예상 시작시간 설정
    	        	w.setEstimatedStartTime(startTime);
    	        
    	        if(w.getCompletedTimestamp() == null ) //완료되지 않은 경우만 예상 완료시간 설정
    	        	w.setEstimatedEndTime(endTime);    	        
    	    }
    	    
    	 // 다음 작업의 시작 시간은 현재 작업의 종료 시간 이후
    	    if(maxEndTime != null) maxEndTime.plusSeconds(30);
    	    preStartTime = maxEndTime;
    	}
    	
    	//{{
    	for (Map.Entry<Integer, List<WorkloadTaskWrapper>> entry : orderGroupMap.entrySet()) {
    		entry.getValue().clear();
    	}
    	orderGroupMap.clear();
    	//}}
    	
    }
    
	/**
	 * 해당 컨테이너가 작업 완료 통지를 받을 때  이후의 작업에 대한 부분을 모두 변경한다.
	 * @param t
	 * @param containerKey
	 */
	private void endPod(List<WorkloadTaskWrapper> _wrapperList, WorkloadTaskWrapper _podWrapper) {
		int baseOrder = -1;
		
		List<WorkloadTaskWrapper> wrapperList = _wrapperList;
		baseOrder = _podWrapper.getOrder();  //기준 baseorder		
		
		//order 기준으로 그룹: 자신에 해당하는 order 제외: 동일한 order가 있을 수 있어서 List로 표현
		Map<Integer, List<WorkloadTaskWrapper>> orderGroupMap = filterAndGroupByOrder(wrapperList, baseOrder, false);
		if(orderGroupMap.isEmpty()) {
			return;
		}
		
		LocalDateTime startTime = null;
		LocalDateTime endTime = null;
		LocalDateTime maxEndTime = null;  //동일한 order일 경우 최대값을 기준으로 다음 실행시간을 결정필요
		
		startTime = _podWrapper.getCompletedTimestamp();
		
		for (Map.Entry<Integer, List<WorkloadTaskWrapper>> entry : orderGroupMap.entrySet()) {
			//order = entry.getKey();
			List<WorkloadTaskWrapper> wrappers = entry.getValue();
			
			maxEndTime = null;
			for (WorkloadTaskWrapper w : wrappers) {
				startTime = _podWrapper.getScheduledTimestamp();
				// 예상 실행 시간을 기준으로 종료 시간 계산
				endTime = (startTime != null) ? startTime.plusMinutes(w.getPredictedExecutionTime()) : null;

				//동일한 순서에 여러개가 실행될 경우
				if(maxEndTime == null)
					maxEndTime = endTime;
				else if(maxEndTime.isAfter(endTime)) {
					maxEndTime = endTime;
				}
				
				//예상 시작 시간과 종료 시간 설정
				if(w.getScheduledTimestamp() == null ) // 실제 시작시간이 없을때
					w.setEstimatedStartTime(startTime);
				if(w.getCompletedTimestamp() == null ) // 실제 완료시간이 없을때
					w.setEstimatedEndTime(endTime);
			}
			
			// 다음 작업의 시작 시간은 현재 작업의 종료 시간 이후
			if(maxEndTime != null) maxEndTime.plusSeconds(30);
			startTime = maxEndTime;
		}
		
		//{{
		for (Map.Entry<Integer, List<WorkloadTaskWrapper>> entry : orderGroupMap.entrySet()) {
			entry.getValue().clear();
		}
		orderGroupMap.clear();
		//}}
	}
    
	/* setPodInfo를 통해서 설정하므로 의미가 없어서 주석 처리함
    //내부적으로 WorkloadTaskContainerWrapper 리스트에서 _poduid가 있는 데이터 조회 
     
    private WorkloadTaskContainerWrapper getWrapperForLatestPod(List<WorkloadTaskContainerWrapper> _wrapperList, String _podUid) {
    	WorkloadTaskContainerWrapper wrapper = null;
    	PromMetricPod pod = null;
    	
    	if(_podUid != null) {
    		for(int i = 0; i < _wrapperList.size(); i++ ) {
        		wrapper = _wrapperList.get(i);
        		pod = wrapper.getLatestPromMetricPod();
    			if(pod.getPodUid().equals(_podUid)) {
    				return wrapper;
    			}
        	}	
    	}
    	return null;
    }
    */
    
	// order별로 그룹으로 처리하고, 정렬하는 함수, 필터조건으로 동일한 order를 포함할 건가? 아니면 그 위로처리할지 
	private Map<Integer, List<WorkloadTaskWrapper>> filterAndGroupByOrder(
			List<WorkloadTaskWrapper> wrappers, int minOrder, boolean isequal) {
		
		//order 기준 필터링
		return wrappers.stream()
				.filter(isequal ? wrapper -> wrapper.getOrder() >= minOrder : wrapper -> wrapper.getOrder() > minOrder) 
				.collect(Collectors.groupingBy(wrapper -> {
					return wrapper.getOrder();
				  }, TreeMap::new, // 결과를 TreeMap으로 생성하여 키를 정렬
						Collectors.toList()
			    ));
	}
    
    
    /**
     * 실제 배포가 되면 나오는 정보를 설정하고, 기존 cluid와 node가 변경되었는지 확인하고 nodeMap을 수정한다.
     */
    public void setPodInfo(PromMetricPod pmPod) {
    	List<WorkloadTaskWrapper> wrappers = this.mlIdMap.get(pmPod.getMlId());
    	if(wrappers == null)
    		return;

    	String containerName = null;
    	String podName = pmPod.getPod();
    	String podUid  = pmPod.getPodUid();
    	for(WorkloadTaskWrapper w: wrappers) {
    		String oldPodUid = w.getPodUid();
    		if(oldPodUid != null) { //등록된 파드가 있으면 podUid만 비교하고 신규로 교체
    			if(oldPodUid.equals(podUid)) {
    				pmPod.setMlContainerNameIdx(w.getNameIdx());
    	    		if(w.getStatus() == PodStatusPhase.RUNNING && pmPod.getStatusPhase() == PodStatusPhase.SUCCEEDED) {
    	    			if(pmPod.getCompletedTimestamp() != null) {
    	    				this.endPod(wrappers, w); //1개가 아니고 리스트 전체를 수정해야 한다.
    	    				w.setStatus(pmPod.getStatusPhase());
    	    			}
    	    		}
    				break;
    			}
    		}else { //등록된 파드가 없으면 신규 등록
	    		containerName = w.getName();
	    		if(podName.contains(containerName)) {
	    			if(pmPod.getScheduledTimestamp() != null) {
						w.setPromMetricPod(pmPod); //처음이라 등록
						this.startPod(wrappers, w);   //1개가 아니고 리스트 전체를 수정해야 한다.
						
						//요청당시의 컨테너 스펙의 이름을 대신할 인덱스 번호 등록(	비교속도)
		    			pmPod.setMlContainerNameIdx(w.getNameIdx());
					}
	    		
	    			/* 각 파드마다 한번 실행인데 굳이 비교할 필요 없이 등록 처리한다. 
	    			//이때 시작이고, 상태변경처리
	    			if(w.getStatus() == PodStatusPhase.UNSUBMITTED || w.getStatus() == PodStatusPhase.PENDING) {
	    				if(pmPod.getScheduledTimestamp() != null) { //이상태는 PodStatusPhase.RUNNING, 상태가 변경될때만 처리하기 위함
	    					this.startPod(wrappers, w);   //1개가 아니고 리스트 전체를 수정해야 한다.
	    				}
					}
					*/
					
	    			
	    			
	    			break;
	    		}
    		}
    	}
    }
    
    /**
     * WorkloadTaskContainerWrapper 상태가 PodStatusPhase.UNSUBMITTED, PENDING인 데이터 추출
     * @return Map<String, Map<String, WorkloadTaskContainerWrapper>>
     */
    public Map<String, Map<String, WorkloadTaskWrapper>> getNotRunningTasks() {

    	Map<String, Map<String, WorkloadTaskWrapper>> resultMap = null;
    	
    	resultMap = this.nodeMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null) // null 방어 코드
                .collect(Collectors.toMap(
                        Map.Entry::getKey, // cl_uid_node_name
                        entry -> entry.getValue().entrySet().stream()
                                .filter(innerEntry -> innerEntry.getValue() != null 
                                        && ( innerEntry.getValue().getStatus() == PodStatusPhase.UNSUBMITTED 
                                          || innerEntry.getValue().getStatus() == PodStatusPhase.PENDING)
                                 )
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey, // mlid_container-name
                                        Map.Entry::getValue // WorkloadTaskContainerWrapper
                                ))
                ));
    	
    	return resultMap;
    }
    
    /**
     * WorkloadTaskContainerWrapper 상태가 PodStatusPhase.UNSUBMITTED, PENDING, RUNNING 인 데이터 추출
     * @return Map<String, Map<String, WorkloadTaskContainerWrapper>>
     */
    public Map<String, Map<String, WorkloadTaskWrapper>> getNotCompletedTasks() {

    	Map<String, Map<String, WorkloadTaskWrapper>> resultMap = null;
    	
    	resultMap = this.nodeMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null) // null 방어 코드
                .collect(Collectors.toMap(
                        Map.Entry::getKey, // cl_uid_node_name
                        entry -> entry.getValue().entrySet().stream()
                                .filter(innerEntry -> {
                                    WorkloadTaskWrapper wrapper = innerEntry.getValue(); // 값 캐싱
                                    return wrapper != null && 
                                           (wrapper.getStatus() == PodStatusPhase.UNSUBMITTED 
                                         || wrapper.getStatus() == PodStatusPhase.PENDING 
                                         || wrapper.getStatus() == PodStatusPhase.RUNNING);
                                })
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey, // mlid_container-name
                                        Map.Entry::getValue // WorkloadTaskContainerWrapper
                                ))
                ));
    	
    	return resultMap;
    }
    
    /**
     * 특정 노드 목록에서 WorkloadTaskContainerWrapper 객체를 수집하여 List로 반환.
     *
     * @param nodeKeys List<String> - 추출하려는 노드의 키 목록
     * @return List<WorkloadTaskContainerWrapper> - 선택된 노드에서 추출한 객체의 리스트
     */
    public List<WorkloadTaskWrapper> listWorkloadTaskContainerWrappersForNodes(List<String> nodeKeys) {
        List<WorkloadTaskWrapper> resultList = new ArrayList<>();

        for (String nodeKey : nodeKeys) {
            // 특정 노드의 데이터를 가져옴
            ConcurrentHashMap<String, WorkloadTaskWrapper> nodeData = nodeMap.get(nodeKey);
            if (nodeData != null) {
                resultList.addAll(nodeData.values());
            }
        }

        return resultList;
    }
    
/*
    
  	public Map<String, ConcurrentHashMap<String, WorkloadTaskWrapper>> getNodeMap() {
  		return this.nodeMap;
  	}
  	
  	public Map<String, List<WorkloadTaskWrapper>> getmlIdMap() {
  		return this.mlIdMap;
  	}
*/  	
  	public List<WorkloadTaskWrapper> getWorkloadTaskWrapperList(String mlId) {
  		return this.mlIdMap.get(mlId);
  	}
/*
    public Map<String, Map<String,WorkloadTaskWrapper>> getReadOnlyNodeMap() {
    	return Collections.unmodifiableMap(this.nodeMap);    	
    }	
*/  	
  	/**
  	 * RuntimeWorkloaTaskdMap size
  	 * @return
  	 */
	/*public int getWorkloaTaskWrapperSize() {
		return this.nodeMap.size();
	}*/
    
    
  	
  	public void debuglog() {
  		if(queueLog.isDebugEnabled()) {
  			//queueLog.debug("\n*****>\nnodeMpa:{} \nmlIdMap:{} \n*****>\n", nodeMap, mlIdMap);
  			try {
  				String jsonstring = JSONUtil.getJsonstringFromObject(mlIdMap);
				queueLog.debug("\n*****>\nmIdMap:{} \n*****>\n", jsonstring);
			} catch (JsonProcessingException e) {
				queueLog.error("Json변환에러:\n",e);
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
  		}
  	}
}
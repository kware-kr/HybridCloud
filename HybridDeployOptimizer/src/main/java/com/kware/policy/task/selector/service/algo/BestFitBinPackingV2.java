package com.kware.policy.task.selector.service.algo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import com.kware.policy.task.collector.service.vo.Cluster;
import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.common.constant.ResourceConstant;
import com.kware.policy.task.common.constant.StringConstant.PodStatusPhase;
import com.kware.policy.task.common.vo.MinimumRequiredFreeResources;
import com.kware.policy.task.selector.service.config.ResourceWeightProperties.ResourceWeight;
import com.kware.policy.task.selector.service.vo.WorkloadTaskWrapper;

import lombok.extern.slf4j.Slf4j;

//Bin Packing: Best Fit(BF) 알고리즘 클래스
/**
 * 항목들을 하나씩 순서대로 빈에 넣습니다.
	각 항목에 대해, 항목을 넣었을 때 남는 공간이 가장 적은 빈에 넣습니다.
	최적의 빈을 찾기 위해 추가적인 계산이 필요하지만, 빈의 사용 효율이 높습니다.
 */
@Slf4j
public class BestFitBinPackingV2 {

	
    private List<PromMetricNode> nodes = null; // 사용 가능한 노드들의 리스트(필터링이된)
    //private int defaultBestNodeCount = 5;
    
    //key: clid_node(getNodeKey)
    private Map<String, Map<String, WorkloadTaskWrapper>> notRunningWrappers = null; // 노드가 선택되었지만 미반영된 할당된 요청들을 관리
    
    Map<String, Cluster> clusters = null;    
    private ResourceWeight weight = null;
    //노드 운영에 필요한 최소한의 여유공간
    
    MinimumRequiredFreeResources minFree = ResourceConstant.minFreeResources;
    
    // 생성자: 노드 리스트와 요청 맵을 초기화
    public BestFitBinPackingV2(List<PromMetricNode> _nodes, Map<String, Map<String, WorkloadTaskWrapper>> _notRunningWrappers, ResourceWeight _weight) {
        this.nodes = _nodes;
        this.notRunningWrappers = _notRunningWrappers;
        this.weight = _weight;
    }
    
    public void setClusters(Map<String, Cluster> _clusters) {
    	this.clusters = _clusters;
    }
    
    //이 데이터를 결과로 보낼까
    //List<AllocationResult>는 내부에서만 사용하고, 리턴은 List<WorkloadTaskContainerWrapper>
    class AllocationResult {
        LocalDateTime starttime;
        LocalDateTime endtime;  //사용할 필요가 있는지
        WorkloadTaskWrapper wrapper;
        //계산시에 사용하고, 최종적으로는 wrapper에 예상시간, cluid, node를 설정해서 리턴한다.
        Queue<NodeScore> nodeScore;
        
		@Override
		public String toString() {
			return "AllocationResult [starttime=" + starttime + ", endtime=" + endtime + ", order=" + wrapper.getOrder()
					+ ", nodeScore=" + nodeScore + "]";
		}
    }
    
    /**
     * 모든 클러스터에서 클러스터별 워크로드가 실행 가능한 노들리스트 를 가져온다.
     * @param _wrapperList
     * @return
     */    
    public void setClusterNodes(List<WorkloadTaskWrapper>  _wrapperList){
    	
    	//getEstimatedStartTime으로 정렬된 전체 PodStatusPhase.UNSUBMITTED, PENDING, RUNNING
    	List<WorkloadTaskWrapper> unWrappers = this.getWrappersMapToList(notRunningWrappers);
    	HashMap<Integer, List<AllocationResult>> cluterRsMap = new HashMap<Integer, List<AllocationResult>>();
    	
    	log.debug("Request Wrapper{}", _wrapperList);
    	
    	for (Map.Entry<String, Cluster> entry : this.clusters.entrySet()) {
    	    String  id = entry.getKey();
    	    Cluster c = entry.getValue();
    	    
    	    List<AllocationResult> rsList = new ArrayList<>();
    	    LocalDateTime prevStart   = null;
    	    log.debug("==================== <<<   Cluster {}", id);
    	    
    	    LocalDateTime start = null;
			LocalDateTime end   = null;
    	    for(int i = 0 ; i < _wrapperList.size(); i++) {
    			WorkloadTaskWrapper  w =  _wrapperList.get(i);
    			
//    			List<PromMetricNode> tempNodes = null;
    			start = w.getEstimatedStartTime();
    			if(prevStart != null && prevStart.isAfter(start)) {
    				start = end;
    				end = start.plusMinutes(w.getPredictedExecutionTime());
    			}else {
    				end   = w.getEstimatedEndTime();
    			}
    			
    			//이건 나중에 객체를 참초하는 방식을 변경해야겠네, 계속 collection들이 생성되지 않고, 한번 생성한 컬력션을 사용하도록
    			List<WorkloadTaskWrapper> tmpUnWrappers = null;
    			List<WorkloadTaskWrapper> tmpOverlabWrappers = null;
    			List<WorkloadTaskWrapper> tmpExpEndRunningWrappers = null;
    			
    			Queue<NodeScore> sortedNodeScore = null;
    			
    			while(sortedNodeScore == null || sortedNodeScore.isEmpty()) {
    				//현재 클러스터에서 가장빠르게 종료되는 wrappers를 기준으로 처리,
    				//다음 예상시간에 맞는 데이터
    			
    				//notRunningWrappers에서 현재 클러스터에 있는 wrappers만 추출(현재 시간기준)
        			//여기서 시간을 필터링, 예상시작시간 이후의 wrappers 추출
    				tmpUnWrappers = this.getWrapperListByAfterTimeAndCluster(unWrappers, start, c );
    				
    				
    				tmpOverlabWrappers = this.getOverlabWrapperList(tmpUnWrappers, start, end);
    				//시간을 계산할때는 상태가 running인 부분을 찾아서 진행하고, 
    				//스코어 계산할때는 running인 파드는 이미 리소스 사용량이 수집에 들어있어서 제외가 필요하며, 실행시간이 겹치는 wrapper들의 사용량을 합산한다.
    				
    				//start 보다 이전에 완료되는 실행중인 wrapperList: 현재 미적용, allocate에 적용예정  
    				tmpExpEndRunningWrappers = this.getRunningWrapperListForBeforeStartTime(unWrappers, start); 
    				
    				sortedNodeScore = this.allocate(c, this.nodes, w, tmpOverlabWrappers); //순서대로 스코어 순서대로
    				if(!sortedNodeScore.isEmpty()) {
    					AllocationResult rs = new AllocationResult();
    					rs.starttime = start;
    					rs.endtime   = end;
    					rs.nodeScore = sortedNodeScore;
    					rs.wrapper = w;
    					rsList.add(rs);
    					prevStart = start;
    					
    					log.debug("노드선택 {}", rs.toString());
    					
    					tmpUnWrappers.clear();
    					tmpOverlabWrappers.clear();
    					break;
    				}
    				
    				start = this.getNextAvailableTime(tmpUnWrappers, start);
    				if(start == null) { //이런사항이 발생하나?
    					log.debug("getNextAvailableTime return is null\n=> {}", w);
    					break;
    				}else if(start.equals(prevStart)) {
    					start = start.plusMinutes(w.getPredictedExecutionTime());
    				}
    				end   = start.plusMinutes(w.getPredictedExecutionTime());
    				
    				tmpUnWrappers.clear();
					tmpOverlabWrappers.clear();
					tmpExpEndRunningWrappers.clear();
    			}
    		}
    	    log.debug("==================== >>>  Cluster {}\n\n", id);

    	    cluterRsMap.put(c.getUid(), rsList);
    	}
    	
    	//{{ 모든 클러스터에서 가장 빠른 시간 및 가장 스코어가 가장 작은 클러스터 설정: 
    	Integer curScoreClUid = -1;
    	Integer curMinDateTimeClUid = -1;
    	Double minScore = Double.MAX_VALUE;
    	Double minScoreForMinDateTime = 0.0;
    	LocalDateTime minDateTime = null;
    	int workloadSize = _wrapperList.size();
    	for(Map.Entry<Integer, List<AllocationResult>> entry: cluterRsMap.entrySet()) {
    		Double scoreSum = 0.0;
    		LocalDateTime maxEndTime = null;
    		
    		List<AllocationResult> rsList = entry.getValue();
    		if(rsList.size() != workloadSize) {
    			continue;
    		}
    		
   		    for (AllocationResult rs : rsList) { //결과리스트
   		    	if(rs.nodeScore != null) {
   		    		NodeScore ns = rs.nodeScore.peek();  //첫번째만 추출
   		    		
   		    		if(ns == null) { //이런게 나오면 안됨
   		    			scoreSum = 0.0;
   		    			maxEndTime = null;
   		    			break;
   		    		}
   		    		scoreSum += ns.getScore();
   		    		
   		    		if(maxEndTime == null || rs.endtime.isAfter(maxEndTime)) {
   	   		    		maxEndTime = rs.endtime;
   	   		    	}
   		    	}
   		    }
    		if(scoreSum == 0 || maxEndTime == null)
    			continue;
    		
   		    if(minScore > scoreSum) {
   		    	minScore = scoreSum;
   		    	curScoreClUid = entry.getKey(); 
   		    }
   		    
   		    if(minDateTime == null || maxEndTime.isBefore(minDateTime)) {
   		    	minDateTime = maxEndTime;
   		    	curMinDateTimeClUid = entry.getKey(); 
   		    	minScoreForMinDateTime = scoreSum;
   		    }else if( maxEndTime.isEqual(minDateTime)) {
   		    	if(minScoreForMinDateTime > scoreSum) {
   		    		curMinDateTimeClUid = entry.getKey();
   		    		minScoreForMinDateTime = scoreSum;
   		    	}
   		    }
    	}
    	
    	//일단 가장 빠르게 워크로드를 종료할 수 있는 노드로 설정해 볼까?
    	Integer curClUid  = curMinDateTimeClUid;
    	//}}
    	
    	//{{리턴 결과 생성
    	List<AllocationResult> arList = cluterRsMap.get(curClUid);
    	if(arList != null) {
	    	for(AllocationResult ar: arList) {
	    		NodeScore ns = ar.nodeScore.peek();
	    		WorkloadTaskWrapper w = ar.wrapper;    		
	    		w.setEstimatedStartTime(ar.starttime);
	    		w.setClUid(curClUid);
	    		w.setNodeName(ns.node.getNode());
	    	}
    	}
    	//}}
    	
    	//{{ 사용 collection 객체 제거
    	for(Map.Entry<Integer, List<AllocationResult>> entry: cluterRsMap.entrySet()) {
   		    for (AllocationResult rs : entry.getValue()) {
   		    	rs.nodeScore.clear();
   		    }
   		    entry.getValue().clear();
    	}
    	cluterRsMap.clear();
    	//}}
    	
    	    	
    	//return _wrapperList;
    }
    
    
    private LocalDateTime getNextAvailableTime(List<WorkloadTaskWrapper> _sortedWrappers, LocalDateTime start) {
    	int size = _sortedWrappers.size();
        for (WorkloadTaskWrapper wrapper : _sortedWrappers) {
            // 정렬된 리스트이므로 조건을 만족하는 첫 번째 요소가 가장 작은 값임
            if (wrapper.getEstimatedStartTime().isAfter(start)) {
                return wrapper.getEstimatedStartTime();
            }
        }
        // 조건을 만족하는 값이 없는 경우 맨 마직막 end값 반환
        return _sortedWrappers.get(size -1).getEstimatedEndTime();
//        return null;
    }
    

	private List<WorkloadTaskWrapper> getWrappersMapToList(Map<String, Map<String, WorkloadTaskWrapper>> _map){
    	List<WorkloadTaskWrapper> sortedList = _map.values().stream() // 바깥 Map의 값 추출
                .flatMap(innerMap -> innerMap.values().stream()) // 안쪽 Map의 값 추출
                .sorted(Comparator.comparing(WorkloadTaskWrapper::getEstimatedStartTime)) // LocalDateTime으로 정렬
                .collect(Collectors.toList()); // 정렬된 List로 수집
    	
    	return sortedList;
    }
    
     
    /**
     * 제공한 시간과 overlab된 데이터 WorkloadTaskWrapper 리스트를 가져온다.
     * @param _wrappers
     * @param _start
     * @param _end
     * @return
     */
    private List<WorkloadTaskWrapper> getOverlabWrapperList(List<WorkloadTaskWrapper> _unWrappers, LocalDateTime _start, LocalDateTime _end ){
	    List<WorkloadTaskWrapper> ws = new ArrayList<WorkloadTaskWrapper>();
	    for (WorkloadTaskWrapper w : _unWrappers) {
	        if(w.getStatus() == PodStatusPhase.PENDING || w.getStatus() == PodStatusPhase.UNSUBMITTED ){
	        	if(w.overlaps(_start, _end)) {
	        		ws.add(w);	           
	        	}
	        }
	    }
	    return ws;
    }    
    
    
    /**
     * 제공한 시간이전에 종료가 예상되는 실행중인 WorkloadTaskWrapper 리스트를 가져온다.
     * @param _wrappers
     * @param _start
     * @param _end
     * @return
     */
    private List<WorkloadTaskWrapper> getRunningWrapperListForBeforeStartTime(List<WorkloadTaskWrapper> _unWrappers, LocalDateTime _start ){
	    List<WorkloadTaskWrapper> ws = new ArrayList<WorkloadTaskWrapper>();
	    for (WorkloadTaskWrapper w : _unWrappers) {
	        if(w.getStatus() == PodStatusPhase.RUNNING ){
	        	if(_start.isAfter(w.getEstimatedEndTime())) {
	        		ws.add(w);	  
	        	}
	        }
	    }
	    return ws;
    }    
    

    /**
     * 시작시간이 이후이고, 해당 클러스터에 속하는 아직 배포되지 않는 데이터
     * @param _unWrappers
     * @param _ldt
     * @param _cluster
     * @return
     */
    private List<WorkloadTaskWrapper> getWrapperListByAfterTimeAndCluster(List<WorkloadTaskWrapper> _unWrappers, LocalDateTime _ldt, Cluster _cluster ){
	    List<WorkloadTaskWrapper> ws = new ArrayList<WorkloadTaskWrapper>();
	    for (WorkloadTaskWrapper w : _unWrappers) {
	        if (w.getClUid() == _cluster.getUid() && _ldt.isBefore(w.getEstimatedEndTime())) {
	            ws.add(w);	           
	        }
	    }
	    return ws;
    }   
    

    /**
     * 주어진 요청을 처리할 때 가장 적합한 5개의 노드를 찾아 반환,하고, 클러스터 아이디가 있으면 해당하는 클러스터에있는 노드를 대상으로 처리한다.
     * @param WorkloadRequest.Container
     * 
     * @return List<PromMetricNode> 최적의 노드 리스트
     */
    private Queue<NodeScore> allocate(Cluster _cluster, List<PromMetricNode> _nodes, WorkloadTaskWrapper _newWrapper, List<WorkloadTaskWrapper> _unWrappers) {
    	if(this.nodes == null)
    		return null;
    	
        // 우선순위 큐를 사용하여 노드의 스코어를 기준으로 정렬
        Queue<NodeScore> priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(NodeScore::getScore));
        
        //if(log.isDebugEnabled())
        //	log.debug("====================================================================\n>>{},{}", _cluster.getUid(), _newWrapper.getName());
    	
        // 각 노드를 순회하며 요청을 처리할 수 있는지 확인하고 스코어를 계산하여 우선순위 큐에 추가
        for (PromMetricNode node : _nodes) {
        	if(_cluster == null || _cluster.getUid() == node.getClUid()) {
        		
        		if (node.canHandle() && canAccommodatePendingRequests(node, _unWrappers, _newWrapper)) {
	                double score = calculateScore(node, _unWrappers, _newWrapper);
	                priorityQueue.add(new NodeScore(node, score));
	            }
        	}
        }
        
        //if(log.isDebugEnabled())
        //	log.debug("====================================================================>>");
        
        return priorityQueue;
    }
    

    /**
     * 노드가 이미 할당된 요청과 새로운 요청을 수용할 수 있는지 확인
     * WorkloadRequest가 여러개의 파드를 실행하도록 되어 있어서, 실제 배포는 해당 컨테이너가 담당함
     * 워크플로어 동작으로 실제 나중에 소비할 리소스도 포함되어 있어서 계산이 필요하지만 현재는 안됨
     * @param node
     * @param curContainer
     * @return
     */
    private boolean canAccommodatePendingRequests(PromMetricNode node, Collection<WorkloadTaskWrapper> _unWrappers, WorkloadTaskWrapper _wrapper) {
    	Collection<WorkloadTaskWrapper> containers = _unWrappers;
        
        

        //여기는 request
        long totalPendingCpu    = sumResource(containers, c->c.getRequestsCpu()    * c.getMaxReplicas());
        long totalPendingMemory = sumResource(containers, c->c.getRequestsMemory() * c.getMaxReplicas());
        long totalPendingGpu    = sumResource(containers, c->c.getRequestsGpu()    * c.getMaxReplicas());
        long totalPendingDisk   = sumResource(containers, c->c.getRequestsEphemeralStorage() * c.getMaxReplicas());
        
        int  nodeAvailableCpu    = node.getAvailableCpu()    - minFree.getFreeCpuMilliCores();
        long nodeAvailableMemory = node.getAvailableMemory() - minFree.getFreeMemoryBytes();
        int	 nodeAvailableGpu    = node.getAvailableGpu();  
        long nodeAvailableDisk   = node.getAvailableDisk()   - minFree.getFreeDiskSpaceBytes();
        
        return nodeAvailableCpu    >= _wrapper.getLimitsCpu()                + totalPendingCpu    &&
               nodeAvailableMemory >= _wrapper.getLimitsMemory()             + totalPendingMemory &&
               nodeAvailableGpu    >= _wrapper.getLimitsGpu()                + totalPendingGpu    &&
               nodeAvailableDisk   >= _wrapper.getLimitsEphemeralStorage()   + totalPendingDisk;
    }


    /**
     * 현 노드 상태와 ML요청 간의 할당 스코어를 계산
     * @param node PromMetricNode
     * @param request WorkloadRequest
     * @return double 스코어
     */
    private double calculateScore(PromMetricNode node, Collection<WorkloadTaskWrapper> _unWrappers,  WorkloadTaskWrapper _wrapper) {
        Collection<WorkloadTaskWrapper> containers = _unWrappers;
        
        //이미 배포요청이 되었지만 배포는 안된 즉 연산이 이루어진 정보를 합산해서 처리 limit
        /*
        long totalPendingCpu    = sumResource(containers, c->c.getLimitsCpu()    * c.getMaxReplicas());
        long totalPendingMemory = sumResource(containers, c->c.getLimitsMemory() * c.getMaxReplicas());
        long totalPendingGpu    = sumResource(containers, c->c.getLimitsGpu()    * c.getMaxReplicas());
        long totalPendingDisk   = sumResource(containers, c->c.getLimitsEphemeralStorage() * c.getMaxReplicas());
        */
        //여기는 request
        long totalPendingCpu    = sumResource(containers, c->c.getRequestsCpu()    * c.getMaxReplicas());
        long totalPendingMemory = sumResource(containers, c->c.getRequestsMemory() * c.getMaxReplicas());
        long totalPendingGpu    = sumResource(containers, c->c.getRequestsGpu()    * c.getMaxReplicas());
        long totalPendingDisk   = sumResource(containers, c->c.getRequestsEphemeralStorage() * c.getMaxReplicas());
        
        int  nodeAvailableCpu    = node.getAvailableCpu()    - minFree.getFreeCpuMilliCores();
        long nodeAvailableMemory = node.getAvailableMemory() - minFree.getFreeMemoryBytes();
        int	 nodeAvailableGpu    = node.getAvailableGpu();  
        long nodeAvailableDisk   = node.getAvailableDisk()   - minFree.getFreeDiskSpaceBytes();
        
        //최대값 정규화
        /*
        double cpuScore    = (node.getAvailableCpu()    - (request.getTotalLimitCpu()    + totalPendingCpu))    / (double) node.getCapacityCpu();
        double memoryScore = (node.getAvailableMemory() - (request.getTotalLimitMemory() + totalPendingMemory)) / (double) node.getCapacityMemory();
        double diskScore   = (node.getAvailableDisk()   - (request.getTotalLimitDisk()   + totalPendingDisk))   / (double) node.getCapacityDisk();
        double gpuScore    = (node.getAvailableGpu()    - (request.getTotalLimitGpu()    + totalPendingGpu))    / (double) node.getCapacityGpu();
        */
        
        //정규화 제거
        /*
        double cpuScore    = (node.getAvailableCpu()    - (request.getTotalLimitCpu()    + totalPendingCpu))    ;
        double memoryScore = (node.getAvailableMemory() - (request.getTotalLimitMemory() + totalPendingMemory)) ;
        double diskScore   = (node.getAvailableDisk()   - (request.getTotalLimitDisk()   + totalPendingDisk))   ;
        double gpuScore    = (node.getAvailableGpu()    - (request.getTotalLimitGpu()    + totalPendingGpu))    ;
        */
        
        //로그정규화
        double cpuScore    = Math.log( (nodeAvailableCpu    - (_wrapper.getLimitsCpu()              + totalPendingCpu))   + 1) ;
        double memoryScore = Math.log( (nodeAvailableMemory - (_wrapper.getLimitsMemory()           + totalPendingMemory))+ 1) ;
        double gpuScore    = Math.log( (nodeAvailableGpu    - (_wrapper.getLimitsGpu()              + totalPendingGpu))   + 1) ;
        double diskScore   = Math.log( (nodeAvailableDisk   - (_wrapper.getLimitsEphemeralStorage() + totalPendingDisk))  + 1) ;
        
        //20240718 스코어 계산에만 사용중인 networkIO, diskIO 정보를 추가
        double diskIoScore    = node.getUsageDiskRead1m()       + node.getUsageDiskWrite1m();
        double networkIoScore = node.getUsageNetworkReceive1m() + node.getUsageNetworkTransmit1m();
        diskIoScore           = Math.log(diskIoScore);
        networkIoScore        = Math.log(networkIoScore);
        
        DoScore ds = new DoScore(node.getNode(), node.getClUid(), Math.abs(cpuScore), Math.abs(memoryScore) , Math.abs(diskScore), Math.abs(gpuScore) 
        		                                                      , weight.getCpu()   , weight.getMemory()    , weight.getDisk()   , weight.getGpu());
        
        //String buf = "pCpu=" + totalPendingCpu + ", pMemory=" + totalPendingMemory + ", pGpu=" + totalPendingGpu + ", pDisk=" + totalPendingDisk;
        //log.info("!!!Node Score {} \nPending: {}", ds.toString(), buf);
        return ds.getScore();
        

        //return Math.abs(cpuScore) + Math.abs(memoryScore) + Math.abs(diskScore) + Math.abs(gpuScore);
    }
    
    private static long sumResource(Collection<WorkloadTaskWrapper> _wrapperList, ToLongFunction<WorkloadTaskWrapper> mapper) {
        return _wrapperList.stream()
                .filter(Objects::nonNull) // null 체크
                .mapToLong(mapper)
                .sum();
    }

    // 노드와 해당 노드에 대한 스코어를 저장하는 내부 클래스
    private class NodeScore {
        private final PromMetricNode node;
        private final double score;

        public NodeScore(PromMetricNode node, double score) {
            this.node = node;
            this.score = score;
        }

        public PromMetricNode getNode() {
            return node;
        }

        public double getScore() {
            return score;
        }

		@Override
		public String toString() {
			return "NodeScore [node=" + node.getNode() + ", score=" + score + "]";
		}        
    }
    
    private class DoScore{
    	String node;
    	int clUid;
    	double sCpu;
    	double sMmemory;
    	double sDisk;
    	double sGpu;
    	int wCpu;
    	int wMemory;
    	int wDisk;
    	int wGpu;
    	
    	double score;
    	
		public DoScore(String node, int clUid, double sCpu, double sMmemory, double sDisk, double sGpu, int wCpu,
				int wMemory, int wDisk, int wGpu) {
			super();
			this.node = node;
			this.clUid = clUid;
			
			this.sCpu = sCpu;
			this.sMmemory = sMmemory;
			this.sDisk = sDisk;
			this.sGpu = sGpu;
			
			this.wCpu = wCpu;
			this.wMemory = wMemory;
			this.wDisk = wDisk;
			this.wGpu = wGpu;
			
			this.score = sCpu     * wCpu 
		        	   + sMmemory * wMemory 
		        	   + sDisk    * wDisk
		        	   + sGpu     * wGpu;
		}
		
		public double getScore() {
			return score;
		}

		@Override
		public String toString() {
			return "Score [node=" + node + ", clUid=" + clUid + ", score=" + score + ", sCpu=" + sCpu + ", sMmemory=" + sMmemory
					+ ", sDisk=" + sDisk + ", sGpu=" + sGpu + ", wCpu=" + wCpu + ", wMemory=" + wMemory + ", wDisk="
					+ wDisk + ", wGpu=" + wGpu + "]";
		}    	
    }
    
    /*
    public static void main(String[] args) {
        // 노드 리스트를 생성
        List<PromMetricNode> nodes = new ArrayList<>();

        // 노드들을 추가
        // 예: nodes.add(new PromMetricNode(...));

        // WorkloadRequest 생성
        WorkloadRequest request = new WorkloadRequest();
        // request 객체를 설정

        // 외부에서 requestNodeMap을 입력받음
        ConcurrentHashMap<String, Set<WorkloadRequest>> requestNodeMap = new ConcurrentHashMap<>();
        // requestNodeMap을 설정

        BestFitBinPacking binPacking = new BestFitBinPacking(nodes, requestNodeMap);
        List<PromMetricNode> bestFitNodes = binPacking.allocate(request);

        // 최적 노드에 요청을 추가
        if (!bestFitNodes.isEmpty()) {
            String key = bestFitNodes.get(0).getClUid() + "-" + bestFitNodes.get(0).getNoUid();
            requestNodeMap.computeIfAbsent(key, k -> new HashSet<>()).add(request);
        }
    }
    */
}

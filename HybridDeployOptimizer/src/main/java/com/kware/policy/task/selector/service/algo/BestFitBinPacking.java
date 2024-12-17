package com.kware.policy.task.selector.service.algo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.common.constant.ResourceConstant;
import com.kware.policy.task.common.vo.MinimumRequiredFreeResources;
import com.kware.policy.task.selector.service.config.ResourceWeightProperties.ResourceWeight;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Container;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.ResourceDetail;

import lombok.extern.slf4j.Slf4j;

//Bin Packing: Best Fit(BF) 알고리즘 클래스
/**
 * 항목들을 하나씩 순서대로 빈에 넣습니다.
	각 항목에 대해, 항목을 넣었을 때 남는 공간이 가장 적은 빈에 넣습니다.
	최적의 빈을 찾기 위해 추가적인 계산이 필요하지만, 빈의 사용 효율이 높습니다.
 */
@Slf4j
public class BestFitBinPacking {

	
    private List<PromMetricNode> nodes; // 사용 가능한 노드들의 리스트(필터링이된)
    private int defaultBestNodeCount = 5;
    
    //key: clid_node(getNodeKey)
    private Map<String, Map<String, Container>> notApplyRequestNodeMap; // 노드가 선택되었지만 미반영된 할당된 요청들을 관리
    
    private ResourceWeight weight;
    //노드 운영에 필요한 최소한의 여유공간
    
    MinimumRequiredFreeResources minFree = ResourceConstant.minFreeResources;
    
    // 생성자: 노드 리스트와 요청 맵을 초기화
    public BestFitBinPacking(List<PromMetricNode> nodes, Map<String, Map<String, Container>> requestNodeMap, ResourceWeight weight) {
        this.nodes = nodes;
        this.notApplyRequestNodeMap = requestNodeMap;
        this.weight = weight;
    }
    
    
    
    /**
     * 모든 컨테이너가 돌아갈 수 있는 클러스터 선택
     * @param wlRequest
     * @param maxCount
     * @return
     */
    public List<PromMetricNode> getBestCluster(WorkloadRequest wlRequest){
    	/*1 클러스터 없이 해당 컨테이너를 실행가능한 모든 노드에서 스코어별로 계산*/
    	WorkloadRequest.Request req = wlRequest.getRequest();

		List<Container> containers = req.getContainers();
		
		List<List<PromMetricNode>> sel_nodes = new ArrayList<List<PromMetricNode>>();
		
		for(int i = 0 ; i < containers.size(); i++) {
			Container container =  containers.get(i);
			List<PromMetricNode>  tempNodes = this.allocate(container, null); //전체
			sel_nodes.add(tempNodes);
		}

		//모든 값에서 공통으로 들어 있는 클러스터 리스트를 제공
		List<PromMetricNode> selectClusterNodeList = findIntersection(sel_nodes);
		
		for(List l : sel_nodes) {
			l.clear();
		}
		sel_nodes.clear();
		
		//이값이 널이면 
		return selectClusterNodeList;

    }
    
    // n개의 리스트의 교집합을 구하는 메서드
    private List<PromMetricNode> findIntersection(List<List<PromMetricNode>> lists) {
        // 초기 교집합을 첫 번째 리스트로 설정
        List<PromMetricNode> intersection = lists.stream()
        .reduce((listA, listB) -> listA.stream()
                .filter(personA -> listB.stream().anyMatch(personB -> personA.getClUid().equals(personB.getClUid())))
                .collect(Collectors.toList()))
        .orElse(Collections.emptyList());

        return intersection;
    }

    /**
     * 주어진 요청을 처리할 때 가장 적합한 5개의 노드를 찾아 반환,하고, 클러스터 아이디가 있으면 해당하는 클러스터에있는 노드를 대상으로 처리한다.
     * @param WorkloadRequest.Container
     * 
     * @return List<PromMetricNode> 최적의 노드 리스트
     */
    public List<PromMetricNode> allocate(WorkloadRequest.Container curContainer, int maxCount, Integer _clUid) {
    	if(this.nodes == null)
    		return null;
    	
        // 우선순위 큐를 사용하여 노드의 스코어를 기준으로 정렬
        Queue<NodeScore> priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(NodeScore::getScore));
        
        if(log.isDebugEnabled())
        	log.debug("{}=======================================================================================================>>{}", _clUid, curContainer.getName());

        // 각 노드를 순회하며 요청을 처리할 수 있는지 확인하고 스코어를 계산하여 우선순위 큐에 추가
        for (PromMetricNode node : nodes) {
        	if(_clUid == null || _clUid == node.getClUid()) {
	            if (node.canHandle() && canAccommodatePendingRequests(node, curContainer)) {
	                double score = calculateScore(node, curContainer);
	                node.setBetFitScore(score); //스코어를 저장해보자
	                priorityQueue.add(new NodeScore(node, score));
	            }
        	}
        }

        if(log.isDebugEnabled())
        	log.debug("=======================================================================================================>>");
        // 우선순위 큐에서 전체 노드를 추출하여 PromMetricNode 리스트로 반환 즉 정렬수행
        List<PromMetricNode> bestFitNodes = new ArrayList<>();
        if(maxCount == 0) //전체를 가져오고 싶을때
        	maxCount = Integer.MAX_VALUE;
        //우선순위 큐에서 상위 n개의 노드를 추출하여 PromMetricNode 리스트로 반환
        for (int i = 0; i < maxCount && !priorityQueue.isEmpty(); i++) {
            bestFitNodes.add(priorityQueue.poll().getNode());
        }
        priorityQueue.clear();

        return bestFitNodes;
    }
    
    
    public List<PromMetricNode> allocate(WorkloadRequest.Container curContainer, Integer _clUid) {
    	return allocate(curContainer, defaultBestNodeCount, _clUid);
    }

    /**
     * 노드가 이미 할당된 요청과 새로운 요청을 수용할 수 있는지 확인
     * @param node PromMetricNode
     * @param request WorkloadRequest
     * @return boolean 수용 가능 여부
     */
   /*
    private boolean canAccommodatePendingRequests(PromMetricNode node, WorkloadRequest request) {
		String key = node.getKey();
		
//		Map<String, WorkloadRequest.Container> pendingRequests = notApplyRequestNodeMap.getOrDefault(key, new HashMap<>());
//		
//		long totalPendingCpu    = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitCpu).sum();
//		long totalPendingMemory = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitMemory).sum();
//		long totalPendingGpu    = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitGpu).sum();
//		long totalPendingDisk   = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitDisk).sum();

    	Map<String, WorkloadRequest.Container> pendingRequests = notApplyRequestNodeMap.getOrDefault(key, new HashMap<>());
        
        Collection<Container> containers =  pendingRequests.values();
        
        long totalPendingCpu    = sumResource(containers, ResourceDetail::getCpu);
        long totalPendingMemory = sumResource(containers, ResourceDetail::getMemory);
        long totalPendingGpu    = sumResource(containers, ResourceDetail::getGpu);
        long totalPendingDisk   = sumResource(containers, ResourceDetail::getEphemeralStorage);
        
        return node.getAvailableCpu()    >= request.getTotalLimitCpu()    + totalPendingCpu    &&
               node.getAvailableMemory() >= request.getTotalLimitMemory() + totalPendingMemory &&
               node.getAvailableGpu()    >= request.getTotalLimitGpu()    + totalPendingGpu    &&
               node.getAvailableDisk()   >= request.getTotalLimitDisk()   + totalPendingDisk;
    }
    */
    /**
     * 노드가 이미 할당된 요청과 새로운 요청을 수용할 수 있는지 확인
     * WorkloadRequest가 여러개의 파드를 실행하도록 되어 있어서, 실제 배포는 해당 컨테이너가 담당함
     * 워크플로어 동작으로 실제 나중에 소비할 리소스도 포함되어 있어서 계산이 필요하지만 현재는 안됨
     * @param node
     * @param curContainer
     * @return
     */
    private boolean canAccommodatePendingRequests(PromMetricNode node, Container curContainer) {
		String key = node.getKey();

    	Map<String, WorkloadRequest.Container> pendingRequests = notApplyRequestNodeMap.getOrDefault(key, new HashMap<>());
        
        Collection<Container> containers =  pendingRequests.values();
        
        long totalPendingCpu    = sumResource(containers, ResourceDetail::getCpu);
        long totalPendingMemory = sumResource(containers, ResourceDetail::getMemory);
        long totalPendingGpu    = sumResource(containers, ResourceDetail::getGpu);
        long totalPendingDisk   = sumResource(containers, ResourceDetail::getEphemeralStorage);
        
        int  nodeAvailableCpu    = node.getAvailableCpu()    - minFree.getFreeCpuMilliCores();
        long nodeAvailableMemory = node.getAvailableMemory() - minFree.getFreeMemoryBytes();
        int	 nodeAvailableGpu    = node.getAvailableGpu();  
        long nodeAvailableDisk   = node.getAvailableDisk()   - minFree.getFreeDiskSpaceBytes();
        
        ResourceDetail detail = curContainer.getResources().getLimits();
        return nodeAvailableCpu    >= detail.getCpu()                + totalPendingCpu    &&
               nodeAvailableMemory >= detail.getMemory()             + totalPendingMemory &&
               nodeAvailableGpu    >= detail.getGpu()                + totalPendingGpu    &&
               nodeAvailableDisk   >= detail.getEphemeralStorage()   + totalPendingDisk;
    }


    /**
     * 현 노드 상태와 ML요청 간의 할당 스코어를 계산
     * @param node PromMetricNode
     * @param request WorkloadRequest
     * @return double 스코어
     */
    private double calculateScore(PromMetricNode node, WorkloadRequest.Container curContainer) {
        String key = node.getKey();
        Map<String, WorkloadRequest.Container> pendingRequests = notApplyRequestNodeMap.getOrDefault(key, new HashMap<>());
        
        Collection<Container> containers =  pendingRequests.values();
        //이미 배포요청이 되었지만 배포는 안된 즉 연산이 이루어진 정보를 합산해서 처리
        long totalPendingCpu    = sumResource(containers, ResourceDetail::getCpu);
        long totalPendingMemory = sumResource(containers, ResourceDetail::getMemory);
        long totalPendingGpu    = sumResource(containers, ResourceDetail::getGpu);
        long totalPendingDisk   = sumResource(containers, ResourceDetail::getEphemeralStorage);
        
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
        ResourceDetail detail = curContainer.getResources().getLimits();
        
        double cpuScore    = Math.log( (nodeAvailableCpu    - (detail.getCpu()              + totalPendingCpu))   + 1) ;
        double memoryScore = Math.log( (nodeAvailableMemory - (detail.getMemory()           + totalPendingMemory))+ 1) ;
        double gpuScore    = Math.log( (nodeAvailableGpu    - (detail.getGpu()              + totalPendingGpu))   + 1) ;
        double diskScore   = Math.log( (nodeAvailableDisk   - (detail.getEphemeralStorage() + totalPendingDisk))  + 1) ;
        
        //20240718 스코어 계산에만 사용중인 networkIO, diskIO 정보를 추가
        double diskIoScore    = node.getUsageDiskRead1m()       + node.getUsageDiskWrite1m();
        double networkIoScore = node.getUsageNetworkReceive1m() + node.getUsageNetworkTransmit1m();
        diskIoScore           = Math.log(diskIoScore);
        networkIoScore        = Math.log(networkIoScore);
        
        DoScore ds = new DoScore(node.getNode(), node.getClUid(), Math.abs(cpuScore), Math.abs(memoryScore) , Math.abs(diskScore), Math.abs(gpuScore) 
        		                                                      , weight.getCpu()   , weight.getMemory()    , weight.getDisk()   , weight.getGpu());
        
        String buf = "pCpu=" + totalPendingCpu + ", pMemory=" + totalPendingMemory + ", pGpu=" + totalPendingGpu + ", pDisk=" + totalPendingDisk;
        log.info("!!!Node Score {} \nPending: {}", ds.toString(), buf);
        return ds.getScore();
        

        //return Math.abs(cpuScore) + Math.abs(memoryScore) + Math.abs(diskScore) + Math.abs(gpuScore);
    }
    
    private static long sumResource(Collection<Container> containers, ToLongFunction<ResourceDetail> mapper) {
        return containers.stream()
                .map(Container::getResources)
                .map(WorkloadRequest.Resources::getLimits)
                .filter(Objects::nonNull) // null 체크
                .mapToLong(mapper)
                .sum();
    }

    // 노드와 해당 노드에 대한 스코어를 저장하는 내부 클래스
    private static class NodeScore {
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
    }
    
    private static class DoScore{
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

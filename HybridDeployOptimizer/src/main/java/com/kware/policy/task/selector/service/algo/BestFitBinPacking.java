package com.kware.policy.task.selector.service.algo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import com.kware.policy.service.vo.PromMetricNode;
import com.kware.policy.task.selector.service.vo.ResourceWeightProperties.ResourceWeight;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;

import lombok.extern.slf4j.Slf4j;

//Bin Packing: Best Fit(BF) 알고리즘 클래스
/**
 * 항목들을 하나씩 순서대로 빈에 넣습니다.
	각 항목에 대해, 항목을 넣었을 때 남는 공간이 가장 적은 빈에 넣습니다.
	최적의 빈을 찾기 위해 추가적인 계산이 필요하지만, 빈의 사용 효율이 높습니다.
 */
@Slf4j
public class BestFitBinPacking {

	
    private List<PromMetricNode> nodes; // 사용 가능한 노드들의 리스트
    
    //key: clid_node(getNodeKey)
    private Map<String, Set<WorkloadRequest>> notApplyRequestNodeMap; // 노드가 선택되었지만 미반영된 할당된 요청들을 관리
    
    private ResourceWeight weight;

    // 생성자: 노드 리스트와 요청 맵을 초기화
    public BestFitBinPacking(List<PromMetricNode> nodes, Map<String, Set<WorkloadRequest>> requestNodeMap, ResourceWeight weight) {
        this.nodes = nodes;
        this.notApplyRequestNodeMap = requestNodeMap;
        this.weight = weight;
    }

    /**
     * 주어진 요청을 처리할 때 가장 적합한 5개의 노드를 찾아 반환
     * @param request WorkloadRequest
     * @return List<PromMetricNode> 최적의 노드 리스트
     */
    public List<PromMetricNode> allocate(WorkloadRequest request) {
        // 우선순위 큐를 사용하여 노드의 스코어를 기준으로 정렬
        Queue<NodeScore> priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(NodeScore::getScore));

        // 각 노드를 순회하며 요청을 처리할 수 있는지 확인하고 스코어를 계산하여 우선순위 큐에 추가
        for (PromMetricNode node : nodes) {
            if (node.canHandle() && canAccommodatePendingRequests(node, request)) {
                double score = calculateScore(node, request);
                node.setBetFitScore(score); //스코어를 저장해보자
                priorityQueue.add(new NodeScore(node, score));
            }
        }

        // 우선순위 큐에서 상위 5개의 노드를 추출하여 PromMetricNode 리스트로 반환
        List<PromMetricNode> bestFitNodes = new ArrayList<>();
        for (int i = 0; i < 5 && !priorityQueue.isEmpty(); i++) {
            bestFitNodes.add(priorityQueue.poll().getNode());
        }
        
        priorityQueue.clear();

        return bestFitNodes;
    }

    /**
     * 노드가 이미 할당된 요청과 새로운 요청을 수용할 수 있는지 확인
     * @param node PromMetricNode
     * @param request WorkloadRequest
     * @return boolean 수용 가능 여부
     */
    private boolean canAccommodatePendingRequests(PromMetricNode node, WorkloadRequest request) {
        String key = node.getKey();
        Set<WorkloadRequest> pendingRequests = notApplyRequestNodeMap.getOrDefault(key, new HashSet<>());

        long totalPendingCpu    = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitCpu).sum();
        long totalPendingMemory = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitMemory).sum();
        long totalPendingGpu    = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitGpu).sum();
        long totalPendingDisk   = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitDisk).sum();

        return node.getAvailableCpu()    >= request.getTotalLimitCpu()    + totalPendingCpu    &&
               node.getAvailableMemory() >= request.getTotalLimitMemory() + totalPendingMemory &&
               node.getAvailableGpu()    >= request.getTotalLimitGpu()    + totalPendingGpu    &&
               node.getAvailableDisk()   >= request.getTotalLimitDisk()   + totalPendingDisk;
    }

    /**
     * 현 노드 상태와 ML요청 간의 할당 스코어를 계산
     * @param node PromMetricNode
     * @param request WorkloadRequest
     * @return double 스코어
     */
    private double calculateScore(PromMetricNode node, WorkloadRequest request) {
        String key = node.getKey();
        Set<WorkloadRequest> pendingRequests = notApplyRequestNodeMap.getOrDefault(key, new HashSet<>());

        long totalPendingCpu    = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitCpu).sum();
        long totalPendingMemory = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitMemory).sum();
        long totalPendingGpu    = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitGpu).sum();
        long totalPendingDisk   = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitDisk).sum();

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
        double cpuScore    = Math.log( (node.getAvailableCpu()    - (request.getTotalLimitCpu()    + totalPendingCpu))   + 1) ;
        double memoryScore = Math.log( (node.getAvailableMemory() - (request.getTotalLimitMemory() + totalPendingMemory))+ 1) ;
        double diskScore   = Math.log( (node.getAvailableDisk()   - (request.getTotalLimitDisk()   + totalPendingDisk))  + 1) ;
        double gpuScore    = Math.log( (node.getAvailableGpu()    - (request.getTotalLimitGpu()    + totalPendingGpu))   + 1) ;
        
        //20240718 스코어 계산에만 사용중인 networkIO, diskIO 정보를 추가
        double diskIoScore    = node.getUsageDiskRead1m() + node.getUsageDiskWrite1m();
        double networkIoScore = node.getUsageNetworkReceive1m() + node.getUsageNetworkTransmit1m();
        diskIoScore    = Math.log(diskIoScore);
        networkIoScore = Math.log(networkIoScore);
        
        DoScore ds = new DoScore(node.getNode(), node.getClUid(), Math.abs(cpuScore), Math.abs(memoryScore) , Math.abs(diskScore), Math.abs(gpuScore) 
        		                                                      , weight.getCpu()   , weight.getMemory()    , weight.getDisk()   , weight.getGpu());
        
        log.info("!!!Node Score {}", ds.toString());
        return ds.getScore();

        //return Math.abs(cpuScore) + Math.abs(memoryScore) + Math.abs(diskScore) + Math.abs(gpuScore);
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

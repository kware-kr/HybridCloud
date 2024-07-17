package com.kware.policy.task.selector.service.algo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.kware.policy.service.vo.PromMetricNode;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;

//Bin Packing: Best Fit(BF) 알고리즘 클래스
public class BestFitBinPacking {

	
    private List<PromMetricNode> nodes; // 사용 가능한 노드들의 리스트
    
    //key: clid_node(getNodeKey)
    private ConcurrentHashMap<String, Set<WorkloadRequest>> notApplyRequestNodeMap; // 노드가 선택되었지만 미반영된 할당된 요청들을 관리

    // 생성자: 노드 리스트와 요청 맵을 초기화
    public BestFitBinPacking(List<PromMetricNode> nodes, ConcurrentHashMap<String, Set<WorkloadRequest>> requestNodeMap) {
        this.nodes = nodes;
        this.notApplyRequestNodeMap = requestNodeMap;
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
                priorityQueue.add(new NodeScore(node, score));
            }
        }

        // 우선순위 큐에서 상위 5개의 노드를 추출하여 PromMetricNode 리스트로 반환
        List<PromMetricNode> bestFitNodes = new ArrayList<>();
        for (int i = 0; i < 5 && !priorityQueue.isEmpty(); i++) {
            bestFitNodes.add(priorityQueue.poll().getNode());
        }

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
     * 노드와 요청 간의 할당 스코어를 계산
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

        double cpuScore    = (node.getAvailableCpu()    - (request.getTotalLimitCpu()    + totalPendingCpu))    / (double) node.getCapacityCpu();
        double memoryScore = (node.getAvailableMemory() - (request.getTotalLimitMemory() + totalPendingMemory)) / (double) node.getCapacityMemory();
        double diskScore   = (node.getAvailableDisk()   - (request.getTotalLimitDisk()   + totalPendingDisk))   / (double) node.getCapacityDisk();
        double gpuScore    = (node.getAvailableGpu()    - (request.getTotalLimitGpu()    + totalPendingGpu))    / (double) node.getCapacityGpu();

        return Math.abs(cpuScore) + Math.abs(memoryScore) + Math.abs(diskScore) + Math.abs(gpuScore);
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
}

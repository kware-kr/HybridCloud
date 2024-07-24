package com.kware.policy.task.selector.service.algo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.kware.policy.service.vo.PromMetricNode;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;

//Bin Packing: First Fit Decreasing (FFD) 알고리즘 클래스
/*
 * First Fit Decreasing (FFD) 알고리즘:

	입력할 항목들을 내림차순으로 정렬한 후 First Fit 알고리즘을 적용합니다.
	큰 항목들을 먼저 배치하여 공간 낭비를 줄입니다.
	First Fit보다 빈의 사용 효율이 높지만, 정렬 과정이 추가됩니다.
	First Fit (FF) 알고리즘:
		항목들을 하나씩 순서대로 빈에 넣습니다.
		각 항목에 대해, 첫 번째로 수용할 수 있는 빈에 항목을 넣습니다.
		수용 가능한 빈이 없으면 새 빈을 생성하여 항목을 넣습니다.
		구현이 간단하며 실행 속도가 빠릅니다.
 */
public class FirstFitDecreasingBinPacking {

    private List<PromMetricNode> nodes; // 사용 가능한 노드들의 리스트
    private ConcurrentHashMap<String, Set<WorkloadRequest>> requestNodeMap; // 노드에 할당된 요청들을 관리하는 맵

    // 생성자: 노드 리스트와 요청 맵을 초기화
    public FirstFitDecreasingBinPacking(List<PromMetricNode> nodes, ConcurrentHashMap<String, Set<WorkloadRequest>> requestNodeMap) {
        this.nodes = nodes;
        this.requestNodeMap = requestNodeMap;
    }

    /**
     * First Fit Decreasing (FFD) 알고리즘을 사용하여 요청을 처리할 때 가장 적합한 5개의 노드를 찾아 반환
     * @param request WorkloadRequest
     * @return List<PromMetricNode> 최적의 노드 리스트
     */
    public List<PromMetricNode> allocate(WorkloadRequest request) {
        List<PromMetricNode> bestNodes = new ArrayList<>();

        // 노드 스코어의 크기가 큰 순서대로 아이템 정렬
        List<PromMetricNode> sortedNodes = nodes.stream()
                .sorted((n1, n2) -> Double.compare(n2.getScore(), n1.getScore()))
                .collect(Collectors.toList());

        // 용량을 초과하지 않으면서 최초로 맞는 노드를 찾음
        for (PromMetricNode node : sortedNodes) {
            if (node.canHandle() && canAccommodatePendingRequests(node, request)) {
                bestNodes.add(node);
                if (bestNodes.size() >= 5) {
                    break;
                }
            }
        }

        return bestNodes;
    }

    /**
     * 노드가 이미 할당된 요청과 새로운 요청을 수용할 수 있는지 확인
     * @param node PromMetricNode
     * @param request WorkloadRequest
     * @return boolean 수용 가능 여부
     */
    private boolean canAccommodatePendingRequests(PromMetricNode node, WorkloadRequest request) {
        String key = node.getClUid() + "-" + node.getNoUid();
        Set<WorkloadRequest> pendingRequests = requestNodeMap.getOrDefault(key, new HashSet<>());

        long totalPendingCpu    = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitCpu).sum();
        long totalPendingMemory = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitMemory).sum();
        long totalPendingGpu    = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitGpu).sum();
        long totalPendingDisk   = pendingRequests.stream().mapToLong(WorkloadRequest::getTotalLimitDisk).sum();

        return node.getAvailableCpu()     >= request.getTotalLimitCpu()    + totalPendingCpu    &&
                node.getAvailableMemory() >= request.getTotalLimitMemory() + totalPendingMemory &&
                node.getAvailableGpu()    >= request.getTotalLimitGpu()    + totalPendingGpu    &&
                node.getAvailableDisk()   >= request.getTotalLimitDisk()   + totalPendingDisk;
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

        FirstFitDecreasingBinPacking binPacking = new FirstFitDecreasingBinPacking(nodes, requestNodeMap);
        List<PromMetricNode> bestNodes = binPacking.allocate(request);

        // 최적 노드에 요청을 추가
        if (!bestNodes.isEmpty()) {
            String key = bestNodes.get(0).getClUid() + "-" + bestNodes.get(0).getNoUid();
            requestNodeMap.computeIfAbsent(key, k -> new HashSet<>()).add(request);
        }
    }
}

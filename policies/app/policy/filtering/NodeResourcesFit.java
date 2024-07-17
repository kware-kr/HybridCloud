package kware.app.policy.filtering;

import kware.app.node.domain.NodeInfo;
import kware.app.node.domain.Workload;
import kware.app.policy.FilteringPolicy;
import kware.app.policy.Policy;

/**
 * 리소스 가용량 기반 필터링
 * - CPU, 메모리, GPU 등의 요구 사항이 가용 리소스를 초과하지 않는 노드만 허용
 */
public class NodeResourcesFit implements FilteringPolicy {
    @Override
    public boolean apply(Workload workload, NodeInfo nodeInfo) {
        double cpuRequested = workload.getCpuRequested();
        double memoryRequested = workload.getMemoryRequested();
        double gpuRequested = workload.getGpuRequested();

        double cpuAllocatable = nodeInfo.getCpuAllocatable();
        double memoryAllocatable = nodeInfo.getMemoryAllocatable();
        double gpuAllocatable = nodeInfo.getGpuAllocatable();

        return cpuRequested <= cpuAllocatable && memoryRequested <= memoryAllocatable && gpuRequested <= gpuAllocatable;
    }

    @Override
    public String log() {
        return "CPU, 메모리, GPU 등의 요구 사항이 가용 리소스를 초과하지 않는 노드";
    }
}

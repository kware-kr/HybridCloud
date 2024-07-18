package kware.app.policy.scoring;

import kware.app.node.domain.NodeInfo;
import kware.app.node.domain.Workload;
import kware.app.policy.Policy;
import kware.app.policy.ScoringPolicy;

/**
 * 리소스 기반 스코어링
 * - 모든 노드에 파드를 고르게 분산시키는 것을 목적으로 함
 */
public class BalancedResourceAllocation implements ScoringPolicy {
    @Override
    public double calculateScore(Workload workload, NodeInfo nodeInfo) {
        double cpuUsage = nodeInfo.getCpuUsage();
        double cpuCapacity = nodeInfo.getCpuCapacity();

        double memoryUsage = nodeInfo.getMemoryUsage();
        double memoryCapacity = nodeInfo.getMemoryCapacity();

        double gpuUsage = nodeInfo.getGpuUsage();
        double gpuCapacity = nodeInfo.getGpuCapacity();

        double cpuRequested = workload.getCpuRequested();
        double memoryRequested = workload.getMemoryRequested();
        double gpuRequested = workload.getGpuRequested();

        double cpuScore = (cpuCapacity - cpuRequested) / cpuCapacity - cpuUsage;
        double memoryScore = (memoryCapacity - memoryRequested) / memoryCapacity - memoryUsage;
        double gpuScore = (gpuCapacity - gpuRequested) / gpuCapacity - gpuUsage;

        if (Double.isNaN(cpuScore)) {
            cpuScore = 0.0;
        }
        if (Double.isNaN(memoryScore)) {
            memoryScore = 0.0;
        }
        if (Double.isNaN(gpuScore)) {
            gpuScore = 0.0;
        }

        return (cpuScore + memoryScore + gpuScore) / 2;
    }

    @Override
    public String log() {
        return "리소스의 사용률이 가장 균형 있는 노드";
    }
}

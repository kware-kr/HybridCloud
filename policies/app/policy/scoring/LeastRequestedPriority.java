package kware.app.policy.scoring;

import kware.app.node.domain.NodeInfo;
import kware.app.node.domain.Workload;
import kware.app.policy.ScoringPolicy;

/**
 * 리소스 사용량 기반 스코어링
 * - 리소스가 가장 적은 노드에게 높은 점수를 줘 워크로드를 분산시킴
 */
public class LeastRequestedPriority implements ScoringPolicy {
    @Override
    public double calculateScore(Workload workload, NodeInfo nodeInfo) {
        double cpuUsage = nodeInfo.getCpuUsage();
        double memoryUsage = nodeInfo.getMemoryUsage();
        double gpuUsage = nodeInfo.getGpuUsage();

        double cpuScore = 1 - cpuUsage;
        double memoryScore = 1 - memoryUsage;
        double gpuScore = 1 - gpuUsage;

        return (cpuScore + memoryScore + gpuScore) / 3;
    }

    @Override
    public String log() {
        return "리소스가 가장 적은 노드";
    }
}

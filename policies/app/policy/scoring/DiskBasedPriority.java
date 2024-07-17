package kware.app.policy.scoring;

import kware.app.node.domain.NodeInfo;
import kware.app.node.domain.Workload;
import kware.app.policy.ScoringPolicy;

/**
 * 디스크 사용량 및 I/O 처리 시간 기반 스코어링
 * - 디스크 사용량이 낮고, 디스크 I/O 처리 시간이 짧은 노드에게 높은 점수를 줌
 */
public class DiskBasedPriority implements ScoringPolicy {
    @Override
    public double calculateScore(Workload workload, NodeInfo nodeInfo) {
        double ioScore = 1 / (nodeInfo.getDiskIoTime() + 1e-9);

        double diskAllocatable = nodeInfo.getDiskAllocatable();
        double diskTotal = nodeInfo.getDiskCapacity();
        double diskUsage = 1 - (diskAllocatable / diskTotal);
        double usageScore = 1 - diskUsage;

        return ioScore * usageScore;
    }

    @Override
    public String log() {
        return "디스크 I/O 처리 시간이 짧거나 사용량이 낮은 노드";
    }
}
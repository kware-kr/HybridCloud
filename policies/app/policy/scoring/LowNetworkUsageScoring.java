package kware.app.policy.scoring;

import kware.app.node.domain.NodeInfo;
import kware.app.node.domain.Workload;
import kware.app.policy.Policy;
import kware.app.policy.ScoringPolicy;

/**
 * 네트워크 사용량 기반 스코어링
 * - 낮은 네트워크 사용량 및 패킷 처리량을 가진 노드에 더 높은 점수를 부여
 */
public class LowNetworkUsageScoring implements ScoringPolicy {

    @Override
    public double calculateScore(Workload workload, NodeInfo nodeInfo) {
        double totalNetworkUsage = nodeInfo.getNetworkReceiveBytes() + nodeInfo.getNetworkTransmitBytes();
        double totalPacketRate = nodeInfo.getNetworkReceivePackets() + nodeInfo.getNetworkTransmitPackets();
        return -totalNetworkUsage - totalPacketRate;
    }

    @Override
    public String log() {
        return "낮은 네트워크 사용량 및 패킷 처리량을 가진 노드";
    }
}
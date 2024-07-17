package kware.app.policy.filtering;

import kware.app.node.domain.NodeInfo;
import kware.app.node.domain.Workload;
import kware.app.policy.FilteringPolicy;
import kware.app.policy.Policy;

import javax.validation.constraints.NotNull;

/**
 * 네트워크 오류 발생률 기반 필터링
 * - 기준을 두고, 기준보다 낮은 오류 발생률을 가진 노드만 허용
 */
public class LowNetworkErrorRateFiltering implements FilteringPolicy {
    private final double maxErrorRate;

    public LowNetworkErrorRateFiltering(double maxErrorRate) {
        this.maxErrorRate = maxErrorRate;
    }

    public LowNetworkErrorRateFiltering() {
        this.maxErrorRate = 10.0;
    }

    @Override
    public boolean apply(Workload workload, NodeInfo nodeInfo) {
        double errorRate = nodeInfo.getNetworkReceiveErrs() + nodeInfo.getNetworkTransmitErrs();
        return errorRate <= maxErrorRate;
    }

    @Override
    public String log() {
        final String text = "기준(%.2f)보다 낮은 오류 발생률을 가진 노드";
        return String.format(text, maxErrorRate);
    }
}
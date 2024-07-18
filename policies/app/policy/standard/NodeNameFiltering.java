package kware.app.policy.standard;

import kware.app.node.domain.NodeInfo;
import kware.app.node.domain.Workload;
import kware.app.policy.FilteringPolicy;

/**
 * 노드 이름 기반 필터링
 * - 이름 일치하는 노드만 허용
 */
public class NodeNameFiltering implements FilteringPolicy {
    @Override
    public boolean apply(Workload workload, NodeInfo nodeInfo) {
        if (workload.getNodeName() == null) {
            return true;
        }
        return nodeInfo.getNodeName().equals(workload.getNodeName());
    }

    @Override
    public String log() {
        return null;
    }
}

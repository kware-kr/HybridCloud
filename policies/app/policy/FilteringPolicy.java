package kware.app.policy;

import kware.app.node.domain.NodeInfo;
import kware.app.node.domain.Workload;

/**
 * 필터링 정책
 */
public interface FilteringPolicy extends Policy {
    boolean apply(Workload workload, NodeInfo nodeInfo);
}

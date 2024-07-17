package kware.app.policy;

import kware.app.node.domain.NodeInfo;
import kware.app.node.domain.Workload;

/**
 * 스코어링 정책
 */
public interface ScoringPolicy extends Policy {
    double calculateScore(Workload workload, NodeInfo nodeInfo);
}

package kware.app.policy;

import kware.app.policy.filtering.LowNetworkErrorRateFiltering;
import kware.app.policy.filtering.NodeResourcesFit;
import kware.app.policy.scoring.BalancedResourceAllocation;
import kware.app.policy.scoring.DiskBasedPriority;
import kware.app.policy.scoring.LeastRequestedPriority;
import kware.app.policy.scoring.LowNetworkUsageScoring;

import java.util.HashMap;
import java.util.Map;

/**
 * 워크로드로부터 읽어낼 정책 종류 (필터링+스코어링)
 */
public enum PolicyType {
    LOW_NETWORK_ERROR_RATE_FILTERING(new LowNetworkErrorRateFiltering()),
    NODE_RESOURCES_FIT(new NodeResourcesFit()),
    BALANCED_RESOURCE_ALLOCATION(new BalancedResourceAllocation()),
    LEAST_REQUESTED_PRIORITY(new LeastRequestedPriority()),
    LOW_NETWORK_USAGE_SCORING(new LowNetworkUsageScoring()),
    DISK_BASED_PRIORITY(new DiskBasedPriority());

    private static final Map<String, PolicyType> BY_NAME = new HashMap<>();

    static {
        for (PolicyType policyType : values()) {
            BY_NAME.put(policyType.name, policyType);
        }
    }

    public static PolicyType fromName(String name) {
        return BY_NAME.get(name);
    }

    public final Policy policy;
    public final boolean isFiltering;
    public final String name;

    PolicyType(Policy policy) {
        this.policy = policy;
        this.isFiltering = policy instanceof FilteringPolicy;
        this.name = policy.getClass().getSimpleName();
    }
}

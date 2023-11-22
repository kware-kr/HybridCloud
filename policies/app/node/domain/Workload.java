package kware.app.node.domain;

import kware.app.policy.FilteringPolicy;
import kware.app.policy.ScoringPolicy;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class Workload {
    private double cpuRequested;
    private double memoryRequested;
    private double gpuRequested;

    private String nodeName;
    private Map<String, String> nodeSelector;

    private List<FilteringPolicy> filteringPolicies;
    private List<ScoringPolicy> scoringPolicies;

    public void addFilteringPolicy(FilteringPolicy filteringPolicy) {
        if (filteringPolicies == null) {
            filteringPolicies = new ArrayList<>();
        }
        filteringPolicies.add(filteringPolicy);
    }

    public void addScoringPolicy(ScoringPolicy scoringPolicy) {
        if (scoringPolicies == null) {
            scoringPolicies = new ArrayList<>();
        }
        scoringPolicies.add(scoringPolicy);
    }
}

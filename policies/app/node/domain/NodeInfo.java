package kware.app.node.domain;

import kware.app.policy.FilteringPolicy;
import kware.app.policy.Policy;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class NodeInfo {
    private String instance;
    private String nodeName;

    private double cpuCapacity;
    private double memoryCapacity;
    private double gpuCapacity;

    private double cpuAllocatable;
    private double memoryAllocatable;
    private double gpuAllocatable;

    private double cpuUsage;
    private double memoryUsage;
    private double gpuUsage;

    private double networkReceiveBytes;
    private double networkTransmitBytes;

    private double networkReceivePackets;
    private double networkTransmitPackets;

    private double networkReceiveErrs;
    private double networkTransmitErrs;

    private double diskCapacity;
    private double diskAllocatable;
    private double diskIoTime;

    private List<String> logs;

    private void addLog(Policy policy) {
        addLog(policy, "");
    }

    public void addLog(Policy policy, String ext) {
        if (logs == null) {
            logs = new ArrayList<>();
        }
        if (policy.log() != null) {
            String log = policy.log() + ext;
            logs.add(log);
        }
    }

    public boolean applyFilteringPolicy(FilteringPolicy filteringPolicy, Workload workload) {
        boolean applied = filteringPolicy.apply(workload, this);
        if (applied) addLog(filteringPolicy);
        return applied;
    }
}

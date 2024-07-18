package kware.app.node;

import com.google.gson.Gson;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import io.kubernetes.client.util.Yaml;
import kware.app.collector.domain.metric.Metric;
import kware.app.collector.domain.metric.MetricDao;
import kware.app.node.domain.NodeInfo;
import kware.app.node.domain.Workload;
import kware.app.policy.FilteringPolicy;
import kware.app.policy.PolicyHelper;
import kware.app.policy.PolicyType;
import kware.app.policy.ScoringPolicy;
import kware.app.policy.standard.NodeNameFiltering;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class NodeInfoManager {

    private final MetricDao metricDao;
    private final PolicyHelper policyHelper;

    /**
     * 메트릭 테이블에서 가장 최근에 수집한 노드 정보 select
     */
    private List<NodeInfo> getNodeInfos() {
        Gson gson = new Gson();
        List<Metric> metrics = metricDao.list();
        return metrics.stream().map(metric -> gson.fromJson(metric.getData(), NodeInfo.class)).collect(Collectors.toList());
    }

    /**
     * 필터링 정책 적용
     */
    private List<NodeInfo> applyFiltering(Workload workload, List<NodeInfo> nodes, List<FilteringPolicy> filteringPolicies) {
        if (filteringPolicies == null) return nodes;
        return nodes.stream()
                .filter(node -> filteringPolicies.stream().allMatch(policy -> node.applyFilteringPolicy(policy, workload)))
                .collect(Collectors.toList());
    }

    /**
     * 스코어링 정책 적용
     */
    private Map<NodeInfo, Double> applyScoring(Workload workload, List<NodeInfo> nodes,
                                               List<ScoringPolicy> scoringPolicies, Map<String, Double> policiesWithWeight) {
        Map<NodeInfo, Double> totalScores = new HashMap<>();

        for (ScoringPolicy scoringPolicy : ListUtils.emptyIfNull(scoringPolicies)) {
            String scoringPolicyName = scoringPolicy.getClass().getSimpleName();
            double weight = policiesWithWeight.getOrDefault(scoringPolicyName, 1.0);

            Map<NodeInfo, Double> scores = policyHelper.normalizeScores(workload, nodes, scoringPolicy, weight);
            for (NodeInfo nodeInfo : scores.keySet()) {
                double total = totalScores.getOrDefault(nodeInfo, 0.0);
                double current = scores.get(nodeInfo);
                totalScores.put(nodeInfo, total + current);
            }
        }

        return totalScores;
    }

    /**
     * 문자열로부터 워크로드를 읽음
     */
    public Workload loadWorkload(String yamlStr, Set<String> policyNames) {
        final String errMsg = "잘못된 kubernetes yaml 파일 형식입니다.";

        V1Pod pod = Yaml.loadAs(yamlStr, V1Pod.class);

        if (pod.getSpec() == null) {
            throw new RuntimeException(errMsg);
        }

        V1Container container = pod.getSpec().getContainers().get(0);
        V1ResourceRequirements resources = container.getResources();

        if (resources == null || resources.getRequests() == null) {
            throw new RuntimeException(errMsg);
        }

        Map<String, Quantity> requests = resources.getRequests();
        Quantity zero = new Quantity(BigDecimal.ZERO, Quantity.Format.DECIMAL_SI);

        double cpuRequested = requests.getOrDefault("cpu", zero).getNumber().doubleValue();
        double memoryRequested = requests.getOrDefault("memory", zero).getNumber().doubleValue();
        double gpuRequested = requests.getOrDefault("nvidia.com/gpu", zero).getNumber().doubleValue();

        String nodeName = pod.getSpec().getNodeName();
        Map<String, String> nodeSelector = pod.getSpec().getNodeSelector();

        Workload workload = Workload.builder()
                .cpuRequested(cpuRequested)
                .memoryRequested(memoryRequested)
                .gpuRequested(gpuRequested)
                .nodeName(nodeName)
                .nodeSelector(nodeSelector)
                .build();

        if (policyNames != null) {
            policyNames.forEach(policyName -> {
                PolicyType policyType = PolicyType.fromName(policyName);
                if (policyType != null) {
                    if (policyType.isFiltering) {
                        workload.addFilteringPolicy((FilteringPolicy) policyType.policy);
                    } else {
                        workload.addScoringPolicy((ScoringPolicy) policyType.policy);
                    }
                }
            });
        }

        return workload;
    }

    /**
     * 가중치를 곱한 점수로 최적의 노드 선정
     */
    public NodeInfo findBestNode(Workload workload, Map<String, Double> policiesWithWeight) {
        // default policies
        workload.addFilteringPolicy(new NodeNameFiltering());

        List<NodeInfo> nodes = getNodeInfos();
        List<NodeInfo> filteredNodes = applyFiltering(workload, nodes, workload.getFilteringPolicies());
        if (filteredNodes.isEmpty()) {
            return null;
        }

        Map<NodeInfo, Double> scoredNodes = applyScoring(workload, filteredNodes, workload.getScoringPolicies(), policiesWithWeight);

        NodeInfo bestNode = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Map.Entry<NodeInfo, Double> entry : scoredNodes.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestNode = entry.getKey();
            }
        }

        return bestNode;
    }
}

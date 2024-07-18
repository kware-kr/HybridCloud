package kware.app.policy;

import kware.app.node.domain.NodeInfo;
import kware.app.node.domain.Workload;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PolicyHelper {

    /**
     * 점수 정규화
     */
    public Map<NodeInfo, Double> normalizeScores(Workload workload, List<NodeInfo> nodeInfos, ScoringPolicy policy, double weight) {
        Map<NodeInfo, Double> scores = new HashMap<>();
        for (NodeInfo nodeInfo : ListUtils.emptyIfNull(nodeInfos)) {
            scores.put(nodeInfo, policy.calculateScore(workload, nodeInfo));
        }

        double minScore = Collections.min(scores.values());
        double maxScore = Collections.max(scores.values());

        final String withBracket = " (%.2f)";

        for (NodeInfo nodeInfo : scores.keySet()) {
            double normalizedScore = (scores.get(nodeInfo) - minScore) / (maxScore - minScore) * weight;
            scores.put(nodeInfo, normalizedScore);
            nodeInfo.addLog(policy, String.format(withBracket, normalizedScore));
        }

        return scores;
    }
}

package com.kware.policy.task.feature.eval;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.feature.service.vo.ClusterFeature;

public class SecurityLevelEvaluator {
	
    // Define kernel version categories (simplified for hardcoding)
    private int getKernelScore(String kernelVersion) {
    	double kv = 0.0;
    	kv = Double.parseDouble(kernelVersion);
    	
    	if (kv > 5.15) {
            return 4; // High security kernel
        } else if (kv >= 5.10 && kv <= 5.15) {
            return 3; // High security kernel
        } else if (kv >= 5.4 && kv <= 4.19) {
            return 2; // Medium security kernel
        } else {
            return 1; // Low security kernel
        }
    }

    // Define OS categories and scores
    private int getOSScore(String osName, String osVersion) {
    	return 2;
    }

    // Define cloud type scores
    private static int getCloudScore(String cloudType) {
        switch (cloudType) {
            case "PUB":
                return 3; // Public cloud
            case "PRI":
                return 4; // Private cloud
            case "ONP":
                return 5; // On-premise
            default:
                return 1; // Unknown or insecure cloud type
        }
    }

    // Calculate the overall security level (1 to 5)
    private  int calculateSecurityLevel(String kernelVersion, String osName, String osVersion, String cloudType) {
        int kernelScore = getKernelScore(kernelVersion);
        int osScore     = getOSScore(osName, osVersion);
        int cloudScore  = getCloudScore(cloudType);

        // Weighted average approach (weights can be adjusted as needed)
        double weightedScore = (kernelScore * 0.3) + (osScore * 0.4) + (cloudScore * 0.3);

        // Map weighted score to a security level (1 to 5)
        if (weightedScore >= 4) {
            return 5;
        } else if (weightedScore >= 3) {
            return 4;
        } else if (weightedScore >= 2) {
            return 3;
        } else if (weightedScore >= 1) {
            return 2;
        } else {
            return 1;
        }
    }

	public Map<String, Integer> calculateSecurityLevel(List<PromMetricNode> lastNodes,
			Map<Integer, ClusterFeature> clusterFeature) {

		Map<String, Integer> rsMap = new HashMap<String, Integer>();

		for (PromMetricNode n : lastNodes) {
			Map<String, String> labels = n.getLabels();
			String kernel_major = labels.get("feature.node.kubernetes.io/kernel-version.major");
			String kernel_minor = labels.get("feature.node.kubernetes.io/kernel-version.minor");

			String kernel = "0.0";
			if (kernel_major != null && kernel_minor != null) {
				kernel = kernel_major + "." + kernel_minor;
			} else if (kernel_major != null) {
				kernel = kernel_major;
			} else if (kernel_minor != null) {
				kernel = "0" + kernel_minor;
			}

			String osName    = labels.get("feature.node.kubernetes.io/system-os_release.ID");
			String osVersion = labels.get("feature.node.kubernetes.io/system-os_release.VERSION_ID");

			ClusterFeature cf = clusterFeature.get(n.getClUid());
			String cloudType = cf != null ? cf.getCloudType() : "PRI";
			
			int score = calculateSecurityLevel(kernel,osName, osVersion, cloudType);
			rsMap.put(n.getKey(), score);
		}

		return rsMap;
	}
}

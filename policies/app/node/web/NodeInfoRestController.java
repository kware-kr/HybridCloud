package kware.app.node.web;

import kware.app.node.domain.NodeInfo;
import kware.app.node.NodeInfoManager;
import kware.app.node.domain.Workload;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class NodeInfoRestController {

    private final NodeInfoManager nodeInfoManager;

    @PostMapping("/test")
    public String test() {
        return "test";
    }

    @PostMapping("/api/v1/best-node")
    public ResponseEntity<NodeInfo> findBestNode(@RequestBody WorkloadRequestDto workloadRequestDto) {
        Workload workload = nodeInfoManager.loadWorkload(workloadRequestDto.getWorkloadYaml(), workloadRequestDto.getPoliciesWithWeight().keySet());
        NodeInfo bestNode = nodeInfoManager.findBestNode(workload, workloadRequestDto.getPoliciesWithWeight());
        return ResponseEntity.ok(bestNode);
    }
}

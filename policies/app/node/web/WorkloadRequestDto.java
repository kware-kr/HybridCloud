package kware.app.node.web;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class WorkloadRequestDto {
    private String workloadYaml;
    private Map<String, Double> policiesWithWeight;
}

package com.kware.policy.task.feature.service.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class NodeScalingPolicy {
    private ScalingTrigger cpu;
    private ScalingTrigger gpu;
    private ScalingTrigger disk;
    private ScalingTrigger memory;
    private int maxCount;
    private int minCount;
    private String scalingLogic;
    private boolean scalingAt;

    public NodeScalingPolicy() {}

    public NodeScalingPolicy(ScalingTrigger cpu, ScalingTrigger gpu, ScalingTrigger disk, ScalingTrigger memory, 
                         int maxCount, int minCount, String scalingLogic, String scalingAt) {
        this.cpu = cpu;
        this.gpu = gpu;
        this.disk = disk;
        this.memory = memory;
        this.maxCount = maxCount;
        this.minCount = minCount;
        this.scalingLogic = scalingLogic;
        //this.scalingAt = scalingAt;
        this.setScalingAt(scalingAt);
    }
    
    @JsonProperty("scalingAt")
    public void setScalingAt(String scalingAt) {
        // "Yes"는 true, 그 외에는 false로 처리 (필요에 따라 조건을 추가할 수 있음)
        this.scalingAt = "Y".equalsIgnoreCase(scalingAt);
    }

    @Data
    public static class ScalingTrigger {
        private int inTrigger;
        private int outTrigger;
        Boolean scalingAt;

        public ScalingTrigger() {}

        public ScalingTrigger(int inTrigger, int outTrigger) {
            this.inTrigger = inTrigger;
            this.outTrigger = outTrigger;
        }
        
        @JsonProperty("scalingAt")
        public void setApplied(String scalingAt) {
            // "Yes"는 true, 그 외에는 false로 처리 (필요에 따라 조건을 추가할 수 있음)
            this.scalingAt = "Y".equalsIgnoreCase(scalingAt);
        }
    }
 
}

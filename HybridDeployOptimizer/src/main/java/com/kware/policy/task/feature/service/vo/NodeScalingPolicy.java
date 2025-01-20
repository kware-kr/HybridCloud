package com.kware.policy.task.feature.service.vo;

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

    public NodeScalingPolicy() {}

    public NodeScalingPolicy(ScalingTrigger cpu, ScalingTrigger gpu, ScalingTrigger disk, ScalingTrigger memory, 
                         int maxCount, int minCount, String scalingLogic) {
        this.cpu = cpu;
        this.gpu = gpu;
        this.disk = disk;
        this.memory = memory;
        this.maxCount = maxCount;
        this.minCount = minCount;
        this.scalingLogic = scalingLogic;
    }

    @Data
    public static class ScalingTrigger {
        private int inTrigger;
        private int outTrigger;

        public ScalingTrigger() {}

        public ScalingTrigger(int inTrigger, int outTrigger) {
            this.inTrigger = inTrigger;
            this.outTrigger = outTrigger;
        }
    }
 
}

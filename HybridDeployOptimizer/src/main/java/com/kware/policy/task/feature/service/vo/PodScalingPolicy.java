package com.kware.policy.task.feature.service.vo;

import lombok.Data;

@Data
public class PodScalingPolicy {
    private AdjustmentTrigger cpu;
    private AdjustmentTrigger gpu;
    private AdjustmentTrigger disk;
    private AdjustmentTrigger memory;

    public PodScalingPolicy() {}

    public PodScalingPolicy(AdjustmentTrigger cpu, AdjustmentTrigger gpu, AdjustmentTrigger disk, AdjustmentTrigger memory) {
        this.cpu = cpu;
        this.gpu = gpu;
        this.disk = disk;
        this.memory = memory;
    }

   @Data
    public static class AdjustmentTrigger {
        private int upTrigger;
        private int downTrigger;
        private int adjustmentRate;
        private int observationPeriod;

        public AdjustmentTrigger() {}

        public AdjustmentTrigger(int upTrigger, int downTrigger, int adjustmentRate, int observationPeriod) {
            this.upTrigger = upTrigger;
            this.downTrigger = downTrigger;
            this.adjustmentRate = adjustmentRate;
            this.observationPeriod = observationPeriod;
        }
    }
}

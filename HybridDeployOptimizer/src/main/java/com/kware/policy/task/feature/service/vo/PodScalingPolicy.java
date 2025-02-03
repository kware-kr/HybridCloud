package com.kware.policy.task.feature.service.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PodScalingPolicy {
    private AdjustmentTrigger cpu;
    private AdjustmentTrigger gpu;
    private AdjustmentTrigger disk;
    private AdjustmentTrigger memory;
    
    public static enum TriggerType{
    	CPU, GPU, DISK, MEMORY
    }

    public PodScalingPolicy() {}

    public PodScalingPolicy(AdjustmentTrigger cpu, AdjustmentTrigger gpu, AdjustmentTrigger disk, AdjustmentTrigger memory) {
        this.cpu = cpu;
        this.cpu.setType(TriggerType.CPU);
        
        this.gpu = gpu;
        this.gpu.setType(TriggerType.GPU);
        
        this.disk = disk;
        this.gpu.setType(TriggerType.DISK);
        
        this.memory = memory;
        this.memory.setType(TriggerType.MEMORY);
    }

   @Data
    public static class AdjustmentTrigger {
        private int upTrigger;
        private int downTrigger;
        private int adjustmentRate;
        private int observationPeriod;
        Boolean scalingAt;
        
        private TriggerType type;

        public AdjustmentTrigger() {}

        public AdjustmentTrigger(int upTrigger, int downTrigger, int adjustmentRate, int observationPeriod) {
            this.upTrigger = upTrigger;
            this.downTrigger = downTrigger;
            this.adjustmentRate = adjustmentRate;
            this.observationPeriod = observationPeriod;
        }
        
        @JsonProperty("scalingAt")
        public void setApplied(String scalingAt) {
            // "Yes"는 true, 그 외에는 false로 처리 (필요에 따라 조건을 추가할 수 있음)
            this.scalingAt = "Y".equalsIgnoreCase(scalingAt);
        }
    }
}

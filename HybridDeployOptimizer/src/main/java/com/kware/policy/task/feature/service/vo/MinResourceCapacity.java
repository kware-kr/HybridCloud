package com.kware.policy.task.feature.service.vo;

import lombok.Data;

@Data
public class MinResourceCapacity {
    private String gpu;
    private int cpuCores;
    private int diskSpace;
    private int memorySize;

    // 기본 생성자
    public MinResourceCapacity() {}

    // 모든 필드를 포함한 생성자
    public MinResourceCapacity(String gpu, int cpuCores, int diskSpace, int memorySize) {
        this.gpu = gpu;
        this.cpuCores = cpuCores;
        this.diskSpace = diskSpace;
        this.memorySize = memorySize;
    }

}

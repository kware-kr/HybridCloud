package com.kware.policy.task.common.vo;
/**
 * 리눅스등 운영체제가 가져야할 최소한의 리소스 양을 설정.
 * 현재는 cpu, memory, disk 정도만 있지만, 더 확장 가능
 */

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MinimumRequiredFreeResources {
    // DB 저장용 (core, MB, GB)
    private int freeCpuCores     = 2;           // 기본값: 최소 여유 CPU 2 코어
    private long freeMemoryMB    = 2048;       // 기본값: 최소 여유 메모리 2GB (2048MB)
    private long freeDiskSpaceGB = 20;      // 기본값: 최소 여유 디스크 20GB

    // 연산용 (밀리코어, 바이트)
    private int freeCpuMilliCores;           // 밀리코어 단위로 저장
    private long freeMemoryBytes;            // 바이트 단위로 저장
    private long freeDiskSpaceBytes;        // 바이트 단위로 저장

    // 기본값 설정
    public MinimumRequiredFreeResources() {
        this.setFreeCpuCores(this.freeCpuCores);
        this.setFreeMemoryMB(this.freeMemoryMB);
        this.setFreeDiskSpaceGB(this.freeDiskSpaceGB);
    }

    // 3개의 값을 동시에 설정하는 생성자
    public void setMinimumRequiredFreeResources(int freeCpuCores, long freeMemoryMB, long freeDiskSpaceGB) {
        this.setFreeCpuCores(freeCpuCores);
        this.setFreeMemoryMB(freeMemoryMB);
        this.setFreeDiskSpaceGB(freeDiskSpaceGB);
    }

    // 여유 CPU 코어 설정 시 자동으로 밀리코어로 변환
    public void setFreeCpuCores(int freeCpuCores) {
        this.freeCpuCores = freeCpuCores;
        this.freeCpuMilliCores = freeCpuCores * 1000; // 밀리코어로 변환
    }

    // 여유 메모리(MB) 설정 시 자동으로 바이트로 변환
    public void setFreeMemoryMB(long freeMemoryMB) {
        this.freeMemoryMB = freeMemoryMB;
        this.freeMemoryBytes = freeMemoryMB * 1024 * 1024; // 바이트로 변환
    }

    // 여유 디스크 공간(GB) 설정 시 자동으로 바이트로 변환
    public void setFreeDiskSpaceGB(long freeDiskSpaceGB) {
        this.freeDiskSpaceGB = freeDiskSpaceGB;
        this.freeDiskSpaceBytes = freeDiskSpaceGB * 1024 * 1024 * 1024; // 바이트로 변환
    }
}

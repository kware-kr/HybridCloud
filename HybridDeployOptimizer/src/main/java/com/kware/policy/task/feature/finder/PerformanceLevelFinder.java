package com.kware.policy.task.feature.finder;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PerformanceLevelFinder {

    private List<PerformanceLevel> performanceLevels;

    public PerformanceLevelFinder(String jsonString) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        // JSON String을 읽어서 PerformanceLevel 객체 리스트로 변환
        performanceLevels = objectMapper.readValue(jsonString, new TypeReference<List<PerformanceLevel>>() {});
    }

    public Integer findLevel(long clockSpeed, int coreCount, int memory) {
        for (PerformanceLevel level : performanceLevels) {
            if (isWithinRange(clockSpeed, level.getClockSpeed()) &&
                isWithinRange(coreCount, level.getCoreCount()) &&
                isWithinRange(memory, level.getMemory())) {
                //return level.getName();
                return level.getLevel();
            }
        }
        return -1;
    }

    private boolean isWithinRange(long value, Range range) {
        Long min = range.getMin();
        Long max = range.getMax();
        return (min == null || value >= min) && (max == null || value <= max);
    }

    private boolean isWithinRange(int value, Range range) {
        Long min = range.getMin();
        Long max = range.getMax();
        return (min == null || value >= min) && (max == null || value <= max);
    }
/*
    public static void main(String[] args) {
        try {
            PerformanceLevelFinder finder = new PerformanceLevelFinder("performance_levels.json");
            long clockSpeed = 3500000000L;  // 예시 값
            int coreCount = 30;             // 예시 값
            int memory = 70;                // 예시 값

            Integer level = finder.findLevel(clockSpeed, coreCount, memory);
            System.out.println("Level: " + level);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    */
}

class PerformanceLevel {
    private int level;
    private Range clockSpeed;
    private Range coreCount;
    private Range memory;
    private String name;

    // Getters and setters
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public Range getClockSpeed() { return clockSpeed; }
    public void setClockSpeed(Range clockSpeed) { this.clockSpeed = clockSpeed; }
    public Range getCoreCount() { return coreCount; }
    public void setCoreCount(Range coreCount) { this.coreCount = coreCount; }
    public Range getMemory() { return memory; }
    public void setMemory(Range memory) { this.memory = memory; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

class Range {
    private Long min;
    private Long max;

    // Getters and setters
    public Long getMin() { return min; }
    public void setMin(Long min) { this.min = min; }
    public Long getMax() { return max; }
    public void setMax(Long max) { this.max = max; }
}

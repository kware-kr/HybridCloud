package com.kware.policy.task.selector.service.vo;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.kware.policy.task.selector.service.vo.WorkloadRequest.WorkloadType;

import lombok.Data;
import net.minidev.json.annotate.JsonIgnore;


@Component
@ConfigurationProperties(prefix = "hybrid.policy.resource-weights")
@Data
public class ResourceWeightProperties {

	private final String defaultKey = "ML";
    private ResourceWeight ml  = new ResourceWeight(); // 기본값 설정
    private ResourceWeight dl  = new ResourceWeight(); // 기본값 설정
    private ResourceWeight inf = new ResourceWeight(); // 기본값 설정

    @JsonIgnore  //getter, setter에서 빠지네, @Setter(AccessLevel.NONE), @Getter(AccessLevel.NONE)
    private final Map<String, ResourceWeight> resourceWeightMap = new HashMap<>();
    
    public ResourceWeight getResourceWeight(String key) {
    	if(key == null)
    		return resourceWeightMap.get(defaultKey);
    	return resourceWeightMap.get(key.toUpperCase());
    }
    
    public ResourceWeight getResourceWeight(WorkloadType type) {
    	if(type == null)
    		return resourceWeightMap.get(defaultKey);
    	else {
    		String key = type.toString().toUpperCase();
    		return resourceWeightMap.get(key);
    	}
    }
        
    @PostConstruct
    public void init() {
    	setDefaultValues(ml , new ResourceWeight(4, 2, 3, 4));
        setDefaultValues(dl , new ResourceWeight(3, 5, 3, 4));
        setDefaultValues(inf, new ResourceWeight(4, 2, 3, 3));
        /*
        // ML 기본값 설정
        if (ml.getCpu()      == 0) ml.setCpu(2);
        if (ml.getGpu()      == 0) ml.setGpu(3);
        if (ml.getDisk()     == 0) ml.setDisk(2);
        if (ml.getMemory()   == 0) ml.setMemory(2);

        // DL 기본값 설정
        if (dl.getCpu()      == 0) dl.setCpu(1);
        if (dl.getGpu()      == 0) dl.setGpu(5);
        if (dl.getDisk()     == 0) dl.setDisk(3);
        if (dl.getMemory()   == 0) dl.setMemory(4);

        // INF 기본값 설정
        if (inf.getCpu()    == 0) inf.setCpu(3);
        if (inf.getGpu()    == 0) inf.setGpu(1);
        if (inf.getDisk()   == 0) inf.setDisk(2);
        if (inf.getMemory() == 0) inf.setMemory(3);
        */
     // Map에 값 추가
        resourceWeightMap.put("ML" , ml);
        resourceWeightMap.put("DL" , dl);
        resourceWeightMap.put("INF", inf);
    }
    
    private void setDefaultValues(ResourceWeight actual, ResourceWeight defaults) {
        if (actual.getCpu() == 0)    actual.setCpu(defaults.getCpu());
        if (actual.getGpu() == 0)    actual.setGpu(defaults.getGpu());
        if (actual.getDisk() == 0)   actual.setDisk(defaults.getDisk());
        if (actual.getMemory() == 0) actual.setMemory(defaults.getMemory());
    }

    @Data
    public static class ResourceWeight {
        private int cpu;
        private int gpu;
        private int disk;
        private int memory;

        // 기본 생성자와 값을 받는 생성자
        public ResourceWeight() {}

        public ResourceWeight(int cpu, int gpu, int disk, int memory) {
            this.cpu   = cpu;
            this.gpu   = gpu;
            this.disk  = disk;
            this.memory = memory;
        }

    }
}
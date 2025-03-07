package com.kware.policy.task.selector.service.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PodScalingRequet {
    private String name; //mlId
    private String cluster;
    private List<Container> containers;

    @Data
    public static class Container {
        private String    name;
        private Resources resources;
    }

    @Data
    public static class Resources {
        private ResourceDetail requests;
        private ResourceDetail limits;
    }

    @Data
    public static class ResourceDetail {
        private String cpu;
        private String memory;
        
        @JsonProperty("gpu")
        private String gpu;

        @JsonProperty("ephemeral-storage")
        private String ephemeralStorage;
    }
    
    public void clear() {
    	if(this.containers != null)
    		this.containers.clear();
    	
    	this.containers = null;
    }
}

package com.kware.policy.task.selector.service.vo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

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
    	@JsonInclude(JsonInclude.Include.NON_NULL)
        private String cpu;
    	
    	@JsonInclude(JsonInclude.Include.NON_NULL)
        private String memory;
        
    	@JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("gpu")
        private String gpu;

    	@JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("ephemeral-storage")
        private String ephemeralStorage;
    }
    
    public void clear() {
    	if(this.containers != null)
    		this.containers.clear();
    	
    	this.containers = null;
    }
}

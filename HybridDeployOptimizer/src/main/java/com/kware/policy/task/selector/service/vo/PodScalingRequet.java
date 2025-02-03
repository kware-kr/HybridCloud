package com.kware.policy.task.selector.service.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PodScalingRequet {
    private String name;
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
        private Integer cpu;
        private Long memory;
        
        @JsonProperty("gpu")
        private Integer gpu;

        @JsonProperty("ephemeral-storage")
        private Long ephemeralStorage;
    }
}

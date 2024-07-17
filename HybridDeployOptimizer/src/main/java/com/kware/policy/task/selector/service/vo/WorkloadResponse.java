package com.kware.policy.task.selector.service.vo;

import java.sql.Timestamp;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;


@Data
public class WorkloadResponse {
	private String version;
    private Response response;

    @Data
    public static class Response {
    	private String  id;         //db:ml_uid
    	private String  name;       //workload name
        private String  clusterName; 
        private String  clusterId;  //db:cl_uid
        private String  nodeName;
        private Integer priority;
        private String  preemptionPolicy;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private Date date;
        
        @JsonIgnore
        private String info;     //db:info   json String
        
        //@JsonIgnore
        private String  nodeId;  //db: no_uid
        
        @JsonIgnore
        private Long  uid;       //db: uid
        
        @JsonIgnore
        private Timestamp regDt;
    }
}

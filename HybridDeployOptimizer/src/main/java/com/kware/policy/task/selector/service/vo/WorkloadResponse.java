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
    	//private String  name;       //workload name
    	
    	@JsonIgnore
    	private Integer code;
    	@JsonIgnore
    	private String  message;
    	
    	ResponseResult  result;
    	
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private Date date;
        
        @JsonIgnore
        private String info;     //db:info   json String
        
        @JsonIgnore
        private Long  uid;       //db: uid
        
        @JsonIgnore
        private Timestamp regDt;
                
        @Data
        public static class ResponseResult {
            //private String  clusterName; 
            private String  clusterId;  //db: cl_uid
            private String  nodeName;
            
            //사용가능 불가능 처리
            private Integer priority;
            
            //StringConstant.priorityClass
            //criticalPriority,highPriority,mediumPriority,lowPriority,veryLowPriority 
            private String priorityClass;
            //PreemptLowerPriority|Never
            private String  preemptionPolicy;
            private String  nodeId;    //db: no_uid     
            
        }
        
        private String  originRequest;
    }
}

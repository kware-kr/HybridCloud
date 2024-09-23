package com.kware.policy.task.selector.service.vo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;


@Data
public class WorkloadResponse {
	private String version;
    private Response response;

    @Data
    public static class Response {
    	
    	@JsonIgnore
    	private Integer code;
    	@JsonIgnore
    	private String  message;

    	private String  id;         //db:ml_uid
    	private String  cluster;  //db: cl_uid
    	
    	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private Date date;
    	
    	List<ContainerResult> containers = new ArrayList<ContainerResult>();
    	
    	private String  originRequest;
    	
    	@JsonIgnore //DB상에는 id가 ml_id로 되어있음
        public String getMlId() {
        	return this.id;
        }
        
        public void setMlId(String mlId) {
        	this.id = mlId;
        }
        
        @JsonIgnore //DB상에는 cluster가 cl_uid로 되어있음
        public String getClUid() {
        	return this.cluster;
        }
        
        public void setClUid(String clUid) {
        	this.cluster = clUid;
        }

		public void addContainers(ContainerResult rsResult) {
			containers.add(rsResult);
		}
		
		public void addContainers(String name, String clUid, String node, String priorityClass, String preemptionPolicy) {
			ContainerResult t = new ContainerResult();
			t.setName(name);
			t.setClUid(clUid);
			t.setNode(node);
			t.setPriorityClass(priorityClass);
			t.setPreemptionPolicy(preemptionPolicy);
			
			containers.add(t);
		}
        
        @JsonIgnore
        private String info;     //db:info   json String
        
        @JsonIgnore
        private Long  reqUid;       //db: uid
        
        @JsonIgnore
        private Timestamp regDt;
        
                
        @Data
        public static class ContainerResult {
            private String  name;//container
            @JsonIgnore
        	private String  cluster;  //db: cl_uid
            private String  node;

            //StringConstant.priorityClass
            //criticalPriority,highPriority,mediumPriority,lowPriority,veryLowPriority 
            private String priorityClass;
            //PreemptLowerPriority|Never
            private String  preemptionPolicy;
			/*
			@JsonIgnore
			@JsonProperty("nodeId")
			private String  noUuid;    //db: no_uid
			
			public void setNoUid(String noUid) {
				this.noUuid = noUid;
			}
			
			@JsonIgnore
			public String getNoUid() {
				return this.noUuid;
			}
			*/
            
            @JsonIgnore //DB상에는 cluster가 cl_uid로 되어있음
            public String getClUid() {
            	return this.cluster;
            }
            
            public void setClUid(String clUid) {
            	this.cluster = clUid;
            }
        }
    }
}

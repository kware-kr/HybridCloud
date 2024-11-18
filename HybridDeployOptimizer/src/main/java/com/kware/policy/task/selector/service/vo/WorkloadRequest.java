package com.kware.policy.task.selector.service.vo;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.common.service.vo.CommonQueueDefault;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("unchecked")
@Data
public class WorkloadRequest extends CommonQueueDefault{
    private String  version;
    private Request request;
    private WorkloadResponse.Response response;
    
    @JsonIgnore
    private long complete_notice_militime = 0L;  //배포완료 통지 메시지 받은 시간
    
    @JsonIgnore
    private Integer clUid = null;
    
    @JsonIgnore
    private Integer totalRequestCpu = 0;
    @JsonIgnore
    private Long    totalRequestMemory = 0L;
    @JsonIgnore
    private Integer totalRequestGpu = 0;
    @JsonIgnore
    private Long    totalRequestDisk = 0L;
    @JsonIgnore
    private Integer totalLimitCpu = 0;
    @JsonIgnore
    private Long    totalLimitMemory = 0L;
    @JsonIgnore
    private Integer totalLimitGpu = 0;
    @JsonIgnore
    private Long    totalLimitDisk = 0L;
    
    
	/*   public enum WorkloadType{
		ML, DL, INF    //이 특성에 맞는 가중치를 적용해야하나?
	}
	
	public enum DevOpsType{
		DEV, TEST, PROD //클러스터 설정, 값이 있을 경우만 처리하는 로직
	}*/
        
    @JsonIgnore
    public String getNodeKey(Container container) {
    	return container.getNodeKey();
    }
    
    public void setClUid(Integer clUid) {
    	this.clUid = clUid;
    	//컨테이너에 클래스 ID 설정
    	for(Container c : this.request.getContainers()) {
    		c.setClUid(clUid);
    	}
    }

    @Data
    public static class Request {
    	/**
    	 *  workload 명
    	 */
        private String name;  //db:nm
        /**
         * workload mlId
         */
        private String id;    //db:ml_uid
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private Date date;
        private List<Container> containers;
        private RequestWorkloadAttributes attribute;
        
        @JsonIgnore
        private String requestJson;  //db:info json String
        
        @JsonIgnore
        private String notiJson;  //db:info json String
        
        @JsonIgnore
        /**
         * database unique Id
         */
        private Long uid;     //db:uid
        
        
        
        @JsonIgnore
        private Timestamp requestDt;
        
        @JsonIgnore
        private Timestamp notiDt;
        
        @JsonIgnore
        private Timestamp completeDt;

        
        @JsonIgnore //DB상에는 id가 ml_id로 되어있음
        public String getMlId() {
        	return this.id;
        }
        
        public void setMlId(String mlId) {
        	this.id = mlId;
        }
        
        @JsonIgnore
        public String getRequestKey() {
        	return this.id;
        }
        
        public void setContainers( List<Container> _containers) {
        	this.containers = _containers;
        	for(Container t: containers) {
        		t.setMlId(this.id);
        	}
        };
    }

    @Getter
    @Setter
    public static class Container extends CommonQueueDefault{
        private String name;
        
        private RequestContainerAttributes attribute;
        private Resources resources;
        
        @JsonIgnore
        public String getContainerKey() {
        	return this.mlId + StringConstant.STR_UNDERBAR + name;
        }
        
        @JsonIgnore
        private String mlId;
        
        
        @JsonIgnore
        //노드셀렉터가 선택한 이후에  노드명을 설정
        private String nodeName;
        
        @JsonIgnore
        //노드셀렉터가 선택한 이후에  노드명을 설정
        private Integer clUid;    
        
        @JsonIgnore
        public String getNodeKey() {
        	return this.clUid + StringConstant.STR_UNDERBAR + this.getNodeName();
        }
    }

    @Data
    public static class Resources {
        private ResourceDetail requests;
        private ResourceDetail limits;
        
        
        public void setRequests(ResourceDetail requests) {
        	this.requests= requests;
        	if(this.limits != null) {
        		
        	}
        }
        
        public void setLimitss(ResourceDetail limits) {
        	this.limits = limits;
        	if(this.requests != null) {
        		
        	}
        }
    }

    @Getter
    public static class ResourceDetail {
        private Integer cpu = 0;
        private Long memory = 0L;
        private Integer gpu = 0;
        @JsonProperty("ephemeral-storage")
        private Long ephemeralStorage= 0L;
        
		public void setCpu(String _cpu) {
			this.cpu = convertMetricToLong(_cpu, 0).intValue();
			//this.cpu = _cpu;
		}
		
		public void setMemory(String _memory) {
			this.memory = convertMetricToLong(_memory, 1);
			//this.memory = _memory;
		}
		
		public void setGpu(String _gpu) {
			this.gpu = convertMetricToLong(_gpu, 2).intValue();
			//this.gpu = _gpu;
		}
		
		public void setEphemeralStorage(String _ephemeralStorage) {
			this.ephemeralStorage = convertMetricToLong(_ephemeralStorage, 3);
			//this.ephemeralStorage = _ephemeralStorage;
		}
		
		
		public void setCpu(Integer _cpu) {
			this.cpu = _cpu;
		}
		
		public void setMemory(Long _memory) {
			this.memory = _memory;
		}
		
		public void setGpu(Integer _gpu) {
			this.gpu = _gpu;
		}
		
		public void setEphemeralStorage(Long _ephemeralStorage) {
			this.ephemeralStorage = _ephemeralStorage;
		}
    }

    @Data
    public static class RequestContainerAttributes {
        private Integer      maxReplicas;
        private Integer      totalSize;
        private Integer      predictedExecutionTime;
        private Integer      order;
    }
    
    @Data
    public static class RequestWorkloadAttributes {
        private String     workloadType;
//        private WorkloadType workloadType;  //enum   ML|DL|INF
        private Boolean      isCronJob;
        private String     devOpsType;
//      private DevOpsType   devOpsType;    //enum    DEV|TEST|PROD
        private String       cudaVersion;
        private String       gpuDriverVersion;
        private String       workloadFeature;
        private String       userId;
        private String       yaml;
    }
    
    /** 
     * 다중컨테이너가 있을때 request, limit을 합산 처리
     * 워크플로우나, job의 parallelism 등으로 인하여 순차 처리시에는 맥스 값으로 처리해야 할 수 있다.
     * 200241101 실제 의미없는 함수가 될 수 있음: 작업순서추가로 인함, 동일한 시간에 수행되는 경우도 있지만, 순서대로 처리해야하는 경우 있음.
     */
    public void aggregate(boolean isSum) {
    	if(isSum) {
	        for (Container container : request.getContainers()) {
	            Resources resources = container.getResources();
	            if(resources.getRequests() != null) {
		            totalRequestCpu    += resources.getRequests().getCpu();
		            totalRequestMemory += resources.getRequests().getMemory();
		            totalRequestGpu    += resources.getRequests().getGpu();
		            totalRequestDisk   += resources.getRequests().getEphemeralStorage();
	            }
	            if(resources.getLimits() != null) {
		            totalLimitCpu      += resources.getLimits().getCpu();
		            totalLimitMemory   += resources.getLimits().getMemory();
		            totalLimitGpu      += resources.getLimits().getGpu();
		            totalLimitDisk     += resources.getLimits().getEphemeralStorage();
	            }
	        }
    	}else {
    		for (Container container : request.getContainers()) {
	            Resources resources = container.getResources();
	            if(resources.getRequests() != null) {
		            totalRequestCpu    = Math.max(totalRequestCpu   , resources.getLimits().getCpu());
		            totalLimitMemory   = Math.max(totalRequestMemory, resources.getLimits().getMemory());
		            totalLimitGpu      = Math.max(totalRequestGpu   , resources.getLimits().getGpu());
		            totalLimitDisk     = Math.max(totalRequestDisk  , resources.getLimits().getEphemeralStorage());
	            }
	            if(resources.getLimits() != null) {
		            totalLimitCpu      = Math.max(totalLimitCpu   , resources.getLimits().getCpu());
		            totalLimitMemory   = Math.max(totalLimitMemory, resources.getLimits().getMemory());
		            totalLimitGpu      = Math.max(totalLimitGpu   , resources.getLimits().getGpu());
		            totalLimitDisk     = Math.max(totalLimitDisk  , resources.getLimits().getEphemeralStorage());
	            }
	        }
    	}
        //스코어 연산이나 스케줄링에 필요함
        if(totalLimitCpu    < totalRequestCpu)    totalLimitCpu    = totalRequestCpu;
        if(totalLimitMemory < totalRequestMemory) totalLimitMemory = totalRequestMemory;
        if(totalLimitGpu    < totalRequestGpu)    totalLimitGpu    = totalRequestGpu;
        if(totalLimitDisk   < totalRequestDisk)   totalLimitDisk   = totalRequestDisk;
    }
    
    /**
     * 해당컨테이너의 리소스의 limit, request중에 최대값을 가져오는 함수
     */
    public void getContainerMaxResourceDetail(int containerIndex) {
    	Resources resources = this.request.getContainers().get(containerIndex).getResources();
    	//제한 값과 요청 값 비교 후 더 큰 값으로 설정
    	totalLimitCpu    = Math.max(resources.getLimits().getCpu()             , resources.getRequests().getCpu());
    	totalLimitMemory = Math.max(resources.getLimits().getMemory()          , resources.getRequests().getMemory());
    	totalLimitGpu    = Math.max(resources.getLimits().getGpu()             , resources.getRequests().getGpu());
    	totalLimitDisk   = Math.max(resources.getLimits().getEphemeralStorage(), resources.getRequests().getEphemeralStorage());
    	
    	totalRequestCpu    = totalLimitCpu;    
        totalRequestMemory = totalLimitMemory;
        totalRequestGpu    = totalLimitGpu;   
        totalRequestDisk   = totalLimitDisk;  

    }
    
    
 // 메트릭을 long 형으로 변환하는 유틸리티 메서드, 수정이 필요한 부분
    private static Long convertMetricToLong(String metricValue, int type) {
        if (metricValue == null || metricValue.isEmpty()) {
            return 0L;
        }

        metricValue = metricValue.trim();
        long factor = 1L;

        if (metricValue.endsWith("m")) {
            factor = 1L; //cpu는 밀리코어로
            metricValue = metricValue.substring(0, metricValue.length() - 1);
        } else if (metricValue.toUpperCase().endsWith("MI")) {
        	// 1 MiB = 2^20 bytes
            factor = 1048576; //(long) Math.pow(2, 20);
            metricValue = metricValue.substring(0, metricValue.length() - 2);
        }else {
	        if(type == 0) { //cpu이면
	        	factor = 1000;	
	        }
        }

        try {
            return Long.parseLong(metricValue) * factor;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
 /*   
    public static void main(String[] args) throws IOException {
    	//yml 파싱테스트
    	File f = new File(".");
    	System.out.println(f.getAbsolutePath());
    	
    	YAMLFactory yamlFactory = new YAMLFactory();
        yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        
        ObjectMapper mapper = new ObjectMapper(yamlFactory);
        


        // YAML 파일을 읽어 Java 객체로 변환
        WorkloadResponse response = mapper.readValue(new File("./doc/mlReponse01.yaml"), WorkloadResponse.class);
        WorkloadRequest request   = mapper.readValue(new File("./doc/mlRequest01.yaml"), WorkloadRequest.class);
        
        response = YAMLUtil.read(new File("./doc/mlReponse01.yaml"), WorkloadResponse.class);
        request  = YAMLUtil.read(new File("./doc/mlRequest01.yaml"), WorkloadRequest.class);
        request.aggregate();
        
        response.getResponse().setDate(new Date());
        
        // Java 객체를 다시 String 객체로 변환
        String outputYamlRequest = mapper.writeValueAsString(request);
        System.out.println(outputYamlRequest);
        String outputYamlResponse = mapper.writeValueAsString(response);
        System.out.println(outputYamlResponse);
        
        WorkloadRequest request1 = mapper.readValue(outputYamlRequest, WorkloadRequest.class);
        
        WorkloadResponse response1 = mapper.readValue(outputYamlResponse, WorkloadResponse.class);
        

        
        // Java 객체를 다시 YAML 파일로 변환
        mapper.writeValue(new File("./doc/output_request.yaml"), request1);
        mapper.writeValue(new File("./doc/output_response.yaml"), response1);
        
    	
    	//객체의 hashcode와 equals를 테스트함
    	QueueManager qm = QueueManager.getInstance();
    	WorkloadRequest req0 = new WorkloadRequest();
    	WorkloadRequest req1 = new WorkloadRequest();
    	req0.setClUid(1);
    	req0.setNode("gpu-0");
    	
    	req1.setClUid(1);
    	req1.setNode("gpu-0");
    	req1.setVersion("0.8");
    	
    	WorkloadRequest.Request wRequest0 = new WorkloadRequest.Request();
    	WorkloadRequest.Request wRequest1 = new WorkloadRequest.Request();
    	req0.setRequest(wRequest0);
    	req1.setRequest(wRequest1);
    	
    	req0.request.setId("laksdjlaskjdflaskfj");
    	req1.request.setId("laksdjlaskjdflaskfj2");
    	
    	qm.setWorkloadRequest(req0);
    	qm.setWorkloadRequest(req1);
    	
    	System.out.println("aaaa0:" + wRequest0.hashCode());
    	System.out.println("aaaa1:" + wRequest1.hashCode());

    	System.exit(1);
    }
*/

	@Override
	//request.id만을 비교하여 동일한 객체인지 비교함
	public boolean equals(Object obj) {
		if (this == obj) //메모리 참조
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WorkloadRequest other = (WorkloadRequest) obj;
		return Objects.equals(request.uid, other.request.uid);
	}


	@Override
	public int hashCode() {
		return Objects.hash(request.uid);
	}  
}

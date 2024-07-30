package com.kware.policy.task.selector.service.vo;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kware.policy.task.common.constant.StringConstant;

import lombok.Data;
import lombok.Getter;


@Data
public class WorkloadRequest {
    private String  version;
    private Request request;
    private WorkloadResponse.Response response;
    
    @JsonIgnore
    private Integer clUid = null;
    @JsonIgnore
    private String  node  = null;
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
    
    public enum WorkloadType{
    	ML, DL, INF    //이 특성에 맞는 가중치를 적용해야하나?
    }
    
    public enum DevOpsType{
    	DEV, TEST, PROD //클러스터 설정, 값이 있을 경우만 처리하는 로직
    }
        
    @JsonIgnore
    public String getNodeKey() {
    	return this.clUid + StringConstant.STR_UNDERBAR + this.node;
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
        private List<Container> container;
        private RequestAttributes requestAttributes;
        
        @JsonIgnore
        private String info;  //db:info json String
        
        @JsonIgnore
        /**
         * database unique Id
         */
        private Long uid;     //db:uid
        
        @JsonIgnore
        private Timestamp regDt;
    }

    @Data
    public static class Container {
        private String image;
        private Resources resources;
    }

    @Data
    public static class Resources {
        private ResourceDetail request;
        private ResourceDetail limits;
    }

    @Getter
    public static class ResourceDetail {
        private Integer cpu = 0;
        private Long memory = 0L;
        private Integer gpu = 0;
        @JsonProperty("ephemeral-storage")
        private Long ephemeralStorage= 0L;
        
		public void setCpu(String _cpu) {
			this.cpu = convertMetricToLong(_cpu).intValue();
			//this.cpu = _cpu;
		}
		
		public void setMemory(String _memory) {
			this.memory = convertMetricToLong(_memory);
			//this.memory = _memory;
		}
		
		public void setGpu(String _gpu) {
			this.gpu = convertMetricToLong(_gpu).intValue();
			//this.gpu = _gpu;
		}
		
		public void setEphemeralStorage(String _ephemeralStorage) {
			this.ephemeralStorage = convertMetricToLong(_ephemeralStorage);
			//this.ephemeralStorage = _ephemeralStorage;
		}
    }

    @Data
    public static class RequestAttributes {
        //private String     workloadType;
        private WorkloadType workloadType;  //enum   ML|DL|INF
        private Boolean      isCronJob;
        //private String     devOpsType;
        private DevOpsType   devOpsType;    //enum    DEV|TEST|PROD
        private String       cudaVersion;
        private String       gpuDriverVerion;
        private Integer      maxReplicas;
        private Boolean      isNetworking;
        private Integer      containerImageSize;
        private Integer      predictedExecutionTime;
        private UserInfo     userInfo;
    }

    @Data
    public static class UserInfo {
        private String id;
    }
    
   
    
    
    /**
     * 다중컨테이너가 있을때 request, limit을 합산 처리 
     */
    public void aggregate() {
        for (Container container : request.getContainer()) {
            Resources resources = container.getResources();
            if(resources.getRequest() != null) {
	            totalRequestCpu    += resources.getRequest().getCpu();
	            totalRequestMemory += resources.getRequest().getMemory();
	            totalRequestGpu    += resources.getRequest().getGpu();
	            totalRequestDisk   += resources.getRequest().getEphemeralStorage();
            }
            if(resources.getLimits() != null) {
	            totalLimitCpu      += resources.getLimits().getCpu();
	            totalLimitMemory   += resources.getLimits().getMemory();
	            totalLimitGpu      += resources.getLimits().getGpu();
	            totalLimitDisk     += resources.getLimits().getEphemeralStorage();
            }
        }
        //스코어 여산이나 스케줄링에 필요함
        if(totalLimitCpu    < totalRequestCpu)    totalLimitCpu    = totalRequestCpu;
        if(totalLimitMemory < totalRequestMemory) totalLimitMemory = totalRequestMemory;
        if(totalLimitGpu    < totalRequestGpu)    totalLimitGpu    = totalRequestGpu;
        if(totalLimitDisk   < totalRequestDisk)   totalLimitDisk   = totalRequestDisk;
    }
    
    
 // 메트릭을 long 형으로 변환하는 유틸리티 메서드, 수정이 필요한 부분
    private static Long convertMetricToLong(String metricValue) {
        if (metricValue == null || metricValue.isEmpty()) {
            return 0L;
        }

        metricValue = metricValue.trim();
        long factor = 1L;

        if (metricValue.endsWith("m")) {
            factor = 1L; //cpu는 밀리코어로
            metricValue = metricValue.substring(0, metricValue.length() - 1);
        } else if (metricValue.endsWith("mi") || metricValue.endsWith("MI")) {
        	// 1 MiB = 2^20 bytes
            factor = 1048576; //(long) Math.pow(2, 20);
            metricValue = metricValue.substring(0, metricValue.length() - 2);
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
		return Objects.equals(request.id, other.request.id);
	}


	@Override
	public int hashCode() {
		return Objects.hash(request.id);
	}  
}

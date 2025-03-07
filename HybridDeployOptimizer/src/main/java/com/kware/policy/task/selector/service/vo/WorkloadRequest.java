package com.kware.policy.task.selector.service.vo;

import java.time.LocalDateTime;
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
        private Date date;  //문서상의 요청일자
        private List<Container> containers;
        private RequestWorkloadAttributes attribute;
        
        @JsonIgnore
        private String requestJson;  //db:info json String 요청원본
        
        @JsonIgnore
        private String notiJson;  //db:info json String
        
        @JsonIgnore
        /**
         * database unique Id
         */
        private Long uid;     //db:uid
        
        
        @JsonIgnore
        private Integer clUid = null;
        
        
        //{{
        @JsonIgnore
        private LocalDateTime  deployedAt;    //쿠버네티스 배포 시간(api의 createAt)
        
        @JsonIgnore
        private LocalDateTime  requestDt;     //문서를 수신한 일자
        
        @JsonIgnore
        private LocalDateTime  notiDt;        //배포완료 통지 일자
        
        @JsonIgnore
        private LocalDateTime  completeDt;    //워크로드 완료일자
        //}}

        
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
        
        public void setClUid(Integer clUid) {
        	this.clUid = clUid;
        	//컨테이너에 클래스 ID 설정
        	for(Container c : containers) {
        		c.setClUid(clUid);
        	}
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
        private String name;  //요청컨테이너 이름
        @JsonIgnore
        private Integer nameIdx; //컨테이너리스트에서 번호 즉 한개의 워크로드에 컨테이너가 3개 있으면 번호 내부적으로 생성
        
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

		@Override
		public String toString() {
			return "Container [name=" + name + ", attribute=" + attribute.toString() + ", resources=" + resources.toString() + ", mlId="
					+ mlId + ", nodeName=" + nodeName + ", clUid=" + clUid + "]";
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

		@Override
		public String toString() {
			return "Resources [requests=" + requests.toString() + ", limits=" + limits.toString() + "]";
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

		@Override
		public String toString() {
			return "ResourceDetail [cpu=" + cpu + ", memory=" + memory 
					+ ", gpu=" + gpu + ", ephemeralStorage=" + ephemeralStorage + "]";
		}
    }

    @Data
    public static class RequestContainerAttributes {
        private Integer      maxReplicas;
        private Integer      totalSize;
        private Integer      predictedExecutionTime; //분
        private Integer      order;
        private Boolean      checkpoint = Boolean.FALSE;
        
		@Override
		public String toString() {
			return "RequestContainerAttributes [maxReplicas=" + maxReplicas + ", totalSize=" + totalSize
					+ ", predictedExecutionTime=" + predictedExecutionTime + ", order=" + order 
					+ ", checkpoint="             + checkpoint + "]";
		}
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
        private Boolean      checkpoint = Boolean.FALSE;
        private String       yaml;
        
        
		@Override
		public String toString() {
			return "RequestWorkloadAttributes [workloadType="  + workloadType         + ", isCronJob=" + isCronJob
					+ ", devOpsType="       + devOpsType       + ", cudaVersion="     + cudaVersion 
					+ ", gpuDriverVersion="	+ gpuDriverVersion + ", workloadFeature=" + workloadFeature 
					+ ", userId="           + userId           + ", yaml="            + yaml + "]"
					+ ", checkpoint="       + checkpoint       + "]";
		}
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

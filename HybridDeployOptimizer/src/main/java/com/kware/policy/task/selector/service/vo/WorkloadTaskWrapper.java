package com.kware.policy.task.selector.service.vo;

import java.time.Duration;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.common.constant.StringConstant.PodStatusPhase;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Container;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.RequestContainerAttributes;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.RequestWorkloadAttributes;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.ResourceDetail;

import lombok.Getter;
import lombok.Setter;

/**
 * 이 클래스는 노드셀렉터가 예상시간을 포함한 노드를 선태할 수 있도록 정보를 실시간 갱신하면서 저장
 * WorkloadRequest.Container를 Wrapping하여 실제 노드에 배포된 파드의 실행시간, 예측실행시간, 예측 종료시간정보를
 * 실시간 정보를 통해 저장하고,
 */
@Getter
@Setter
public class WorkloadTaskWrapper {
	// @Setter(AccessLevel.NONE)
	// private Container container; //워크로드 요청시에 제공한 Container 한개의 정보 : 클러스터ID,
	// node_name, mlId가 포함됨: rqeuest, response에서 생성한 정보임
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime estimatedStartTime; // 예상 시작시간
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime estimatedEndTime; // 예상 종료시간
	// @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	// private LocalDateTime actualStartTime; //실제 시작시간
	// @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	// private LocalDateTime actualEndTime; //실제 종료시간

	// private Integer delayMinutes = 1; //default
	
	private String  name; //container name
	private int     nameIdx;  // 이름으로 찾지않고 숫자로 인덱스를 부여함
    private String  mlId;
    private String  nodeName;
    private Integer clUid; 
	
	//{{request
	// Container Resources requests
	private Integer requestsCpu = 0;
	private Long    requestsMemory = 0L;
	private Integer requestsGpu = 0;
	private Long    requestsEphemeralStorage = 0L;

	//Resources > limits
	private Integer limitsCpu = 0;
	private Long    limitsMemory = 0L;
	private Integer limitsGpu = 0;
	private Long    limitsEphemeralStorage = 0L;

	//Container > RequestContainerAttributes
	private Integer maxReplicas = 1;
	private Integer totalSize = 0;
	private Integer predictedExecutionTime = 0; // 분
	private Integer order = 0;
	
	private Boolean enabeldCheckPoint; //체크포인트가 설정되어 있는가?

	//Request > RequestWorkloadAttributes: 동일한 워크로드에 공통사항
	private String workloadType;
	private String devOpsType;
	private String cudaVersion;
	private String gpuDriverVersion;
	private String workloadFeature;

	//}}request
	

	//{{실제 운영중인 PromMetricPod 관련
	private String pod; // pod이름//프로메테우스 resourcename + container.name + randum number고 구성됨
	private String podUid; // 이건 프로메테우스 메트릭에 포함된 정보를 넣으면 어떤가?
	private PodStatusPhase status = PodStatusPhase.UNSUBMITTED;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime createdTimestamp; // 실제 pod 생성시간 1 => 생성하지 pending상태
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime scheduledTimestamp; // 실제 pod 실행시간 2
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime completedTimestamp; // 실제 pod 종료시간 3

	//private PromMetricPod latestPromMetricPod; // 최신 pod 정보
	////}} 실제 운영중인 PromMetricPod 관련

	public WorkloadTaskWrapper(RequestWorkloadAttributes _reqattr, Container _container) {
		// this.container = container;
		this.setContainer(_container);
		this.setRequestWorkloadAttributes(_reqattr);
	}

	public void setPromMetricPod(PromMetricPod _pod) {
		// 상태가 변경되지 않았으면
		if (this.status == _pod.getStatusPhase())
			return;
		
		this.pod    = _pod.getPod();
		this.podUid = _pod.getPodUid();
		this.status = _pod.getStatusPhase();
		
		this.completedTimestamp = _pod.getCompletedTimestamp().toLocalDateTime();
		this.createdTimestamp   = _pod.getCreatedTimestamp().toLocalDateTime();
		this.scheduledTimestamp = _pod.getScheduledTimestamp().toLocalDateTime();
		
		/* 다른 곳에서 
				if (completedTimestamp == null) {
					if (this.scheduledTimestamp != null) {
						this.estimatedEndTime = this.scheduledTimestamp.plusMinutes(this.getPredictedExecutionTime());
					}
				}
		*/
	}

	private void setContainer(Container _container) {
		this.name     = _container.getName();
		this.clUid    = _container.getClUid();
		this.nodeName = _container.getName();
		this.mlId     = _container.getMlId();
		
		ResourceDetail rd = _container.getResources().getRequests();
		this.requestsCpu    = rd.getCpu();
		this.requestsEphemeralStorage = rd.getEphemeralStorage();
		this.requestsGpu    = rd.getGpu();
		this.requestsMemory = rd.getMemory();

		rd = _container.getResources().getLimits();
		this.limitsCpu    = rd.getCpu();
		this.limitsEphemeralStorage = rd.getEphemeralStorage();
		this.limitsGpu    = rd.getGpu();
		this.limitsMemory = rd.getMemory();

		RequestContainerAttributes a = _container.getAttribute();
		this.order       = a.getOrder();
		this.totalSize   = a.getTotalSize();
		this.maxReplicas = a.getMaxReplicas();
		this.predictedExecutionTime = a.getPredictedExecutionTime();
	}

	private void setRequestWorkloadAttributes(RequestWorkloadAttributes reqAttr) {
		this.workloadType     = reqAttr.getWorkloadType();
		this.cudaVersion      = reqAttr.getDevOpsType();
		this.cudaVersion      = reqAttr.getCudaVersion();
		this.gpuDriverVersion = reqAttr.getGpuDriverVersion();
		this.workloadFeature  = reqAttr.getWorkloadFeature();
	}

	@JsonIgnore
    public String getContainerKey() {
    	return this.mlId + StringConstant.STR_UNDERBAR + name;
    }
	
	@JsonIgnore
    public String getNodeKey() {
    	return this.clUid.toString() + StringConstant.STR_UNDERBAR + this.getNodeName();
    }
	
	@JsonIgnore
    public Integer getClusterKey() {
    	return this.clUid;
    }

	/**
	 * 
	 * 중복되는 부분이 있으면 해당 리소스를 합하고, 없으면 적용하지 않는다.
	 * @param otherStart
	 * @param otherEnd
	 * @return
	 */
	public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
		/*
		 * 상태가 UNSUBMITTED, PENDING일 경우에는 예상시간 기준
		 *       RUNNING 일 경우는 start는 scheduledTime end는 예상시간
		 *       SUCCEEDED일 경우는 무조건 false 
		 */
		LocalDateTime base_start, base_end;
		if(this.status == PodStatusPhase.UNSUBMITTED || this.status == PodStatusPhase.PENDING) {
			base_start = this.estimatedStartTime;
			base_end   = this.estimatedEndTime;
		}else if(this.status == PodStatusPhase.RUNNING) {
			base_start = this.scheduledTimestamp;
			base_end   = this.estimatedEndTime;
		}else {
			return false;
		}
		
		Duration  duration;
		// 두 구간의 겹치는 시작 시간과 끝 시간을 계산
        LocalDateTime overlapStart = base_start.isAfter(otherStart) ? base_start : otherStart;
        LocalDateTime overlapEnd   = base_end.isBefore(otherEnd) ? base_end : otherEnd;

        // 겹치는 구간이 있는지 확인
        if (overlapStart.isBefore(overlapEnd) || overlapStart.isEqual(overlapEnd)) {
        	duration = Duration.between(overlapStart, overlapEnd);
        }else {
        	duration = Duration.ZERO;
        }
        
        if(duration.toMinutes() <= 1) {  //겹치지 않는 것으로 간주하는 최소기간:10분=>임시적으로 한거라 추후 수정가능
        	return false;
        }else {
        	return true;
        }
        //return !(this.estimatedEndTime.isBefore(otherStart) || this.estimatedStartTime.isAfter(otherEnd));
    }

	@Override
	public String toString() {
		return "WorkloadTaskWrapper [name=" + name + ", order=" + order + ", status=" + status + ", estimatedStartTime="
				+ estimatedStartTime + ", estimatedEndTime=" + estimatedEndTime + ", predictedExecutionTime="
				+ predictedExecutionTime + ", scheduledTimestamp=" + scheduledTimestamp + ", requestsCpu=" + requestsCpu
				+ ", requestsMemory=" + requestsMemory + ", requestsGpu=" + requestsGpu + ", limitsCpu=" + limitsCpu
				+ ", limitsMemory=" + limitsMemory + ", limitsGpu=" + limitsGpu + ", maxReplicas=" + maxReplicas
				+ ", enabeldCheckPoint=" + enabeldCheckPoint + ", workloadType=" + workloadType + ", workloadFeature="
				+ workloadFeature + ", pod=" + pod + ", mlId=" + mlId + "]";
	}

	
	

	// 실제 promed에 나타나면 해당 데이터의 예상시간 이외에 이후에 처리할 데이터의 예상시간도 처리해야하지만 이건 다른 쪽에서 처리하도록
	// 한다.
	/*
	public void setLatestPromMetricPod(PromMetricPod latestPromMetricPod) {
		this.latestPromMetricPod = latestPromMetricPod;
		
		this.status = latestPromMetricPod.getStatusPhase();
		
	
		if(this.actualStartTime == null) {
			if(this.latestPromMetricPod.getScheduledTimestamp() != null) {
				this.actualStartTime = this.latestPromMetricPod.getScheduledTimestamp().toLocalDateTime();
		    		this.estimatedEndTime =  this.actualStartTime.plusMinutes(this.getPredictedExecutionTime());
			}
		}
		
		if(this.actualEndTime == null) {
			if(this.latestPromMetricPod.getCompletedTimestamp() != null) {
				this.actualEndTime = this.latestPromMetricPod.getCompletedTimestamp().toLocalDateTime();
			}
		}
	}
	*/

}

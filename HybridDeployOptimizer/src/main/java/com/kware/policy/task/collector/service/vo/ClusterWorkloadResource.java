package com.kware.policy.task.collector.service.vo;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
 * 워크로드는 동일한 클러스터에 배포된다.(이것 확인)
 * mlId는 
 */

@Getter
@Setter
@ToString
public class ClusterWorkloadResource extends ClusterDefault {
	private Integer clUid;
	private Integer id;  //기본 ID
	private String nm;
	private String kind;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp   createdAt;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp   updatedAt;
	
	private String uid;
	//private APIConstant.WorkloadStatus status;
	private String status;
	private String resourceId;
	
	private int totalPodCount;
	private int runningPodCount;

	
	@Override
	public void clear() {
		podMap.clear();
		podMap = null;
	}
	
	@JsonIgnore
	@Override
	public String getUniqueKey() {
		//uid를 프로메테우스에서는 가져올 수 없고 name정보만 가져올 수 있다.
		return nm;
	}
		
	// @Setter(AccessLevel.NONE)
	//@Getter(AccessLevel.NONE)
	//pod uid를 key로 함
	@JsonIgnore
	private Map<String, ClusterWorkloadPod> podMap = new HashMap<String, ClusterWorkloadPod>();

	public Map<String, ClusterWorkloadPod> getPods(){
		return this.podMap;
	}
	
	public ClusterWorkloadPod getPod(String _podUid) {
		return podMap.get(_podUid);
	}

	public Boolean containsPod(String _podUid) {
		return podMap.containsKey(_podUid);
	}

	/**
	 * ClusterWorkload에 포함되어 있는 pod를 관리한다.
	 * @param _podUid
	 * @param _pod
	 */
	public void addPod(ClusterWorkloadPod _pod) {
		podMap.put(_pod.getUid(), _pod);
	}
	
	/*
	public void setStatusString(String status) {
		if (status != null && !status.isEmpty()) {
            try {
                this.status =  APIConstant.WorkloadStatus.valueOf(status); // String을 Enum으로 변환
            } catch (IllegalArgumentException e) {
            	this.status = null;
            }
        }
	}	
	
	@JsonIgnore
	public String getStatuString() {
        if (status != null) {
            return status.name(); // Enum의 name() 메서드를 사용해 String으로 변환
        }
        return null;
    }
	*/
}
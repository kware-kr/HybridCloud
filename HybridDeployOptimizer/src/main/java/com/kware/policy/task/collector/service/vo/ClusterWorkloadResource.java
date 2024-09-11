package com.kware.policy.task.collector.service.vo;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kware.policy.task.common.constant.APIConstant;

import lombok.AccessLevel;
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
	private String userId;
	private String info;
	private String memo;
	
	@JsonIgnore
	private String hashVal;
	
	private String mlId;
	private String namespace;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp   createdAt;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp   updatedAt;
	private APIConstant.WorkloadStatus status;
	
	@JsonIgnore
	@Override
	public String getUniqueKey() {
		return mlId;
	}
	
	@Override
	public void clear() {
		mPods.clear();
		mPods = null;
	}
	
	// @Setter(AccessLevel.NONE)
	@Getter(AccessLevel.NONE)
	//pod uid를 key로 함
	private Map<String, ClusterWorkloadPod> mPods = new HashMap<String, ClusterWorkloadPod>();
	
	@Getter(AccessLevel.NONE)
	//resource name을 키로함: 이 키를 기반으로 
	private Map<String, ClusterWorkloadPod> mResources = new HashMap<String, ClusterWorkloadPod>();
	

	public Map<String, ClusterWorkloadPod> getPods(){
		return this.mPods;
	}
	
	public ClusterWorkloadPod getPod(String _podUid) {
		return mPods.get(_podUid);
	}

	public Boolean containsPod(String _podUid) {
		return mPods.containsKey(_podUid);
	}

	/**
	 * ClusterWorkload에 포함되어 있는 pod를 관리한다.
	 * @param _podUid
	 * @param _pod
	 */
	public void addPod(String _podUid, ClusterWorkloadPod _pod) {
		mPods.put(_podUid, _pod);
	}
	
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
}
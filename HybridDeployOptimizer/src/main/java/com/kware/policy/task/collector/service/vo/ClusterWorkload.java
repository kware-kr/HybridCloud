package com.kware.policy.task.collector.service.vo;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kware.common.config.serializer.JsonIgnoreDynamicSerializer;
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
public class ClusterWorkload extends ClusterDefault {
	private Integer clUid;
	private Integer id;  //기본 ID
	private String nm;
	private String userId;
	
	@JsonSerialize(using = JsonIgnoreDynamicSerializer.class) //필요에 따라서 처리함
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
		for (String key : resourceMap.keySet()) {
			ClusterWorkloadResource value = resourceMap.get(key);
			value.clear();
        }
		resourceMap.clear();
		resourceMap = null;
	}
	
	@JsonIgnore
	private Map<String, ClusterWorkloadResource> resourceMap = new HashMap<String, ClusterWorkloadResource>();
	
	public Map<String, ClusterWorkloadResource> getResource(){
		return this.resourceMap;
	}
	
	public boolean addResource(ClusterWorkloadResource _resource) {
		if(_resource.getUniqueKey() == null) {
			return false;
		}
		
		_resource.setClUid(clUid);
		resourceMap.put(_resource.getUniqueKey(),_resource);
		return true;
	}
	
	public ClusterWorkloadResource getResource(String _key) {
		return resourceMap.get(_key);
	}
	
	//{{ ClusterWorkloadResource[ClusterWorkloadPod] 관련
	public Map<String, ClusterWorkloadPod> getPods(String resourceKey){
		ClusterWorkloadResource resource = this.resourceMap.get(resourceKey);
		if(resource != null)
			return resource.getPods();
		else return null;
	}
	
	public ClusterWorkloadPod getPod(String _podUid) {
		ClusterWorkloadPod pod = null;
		for (String key : resourceMap.keySet()) {
			ClusterWorkloadResource value = resourceMap.get(key);
			pod = value.getPod(_podUid);
			if(pod != null) {
				return pod;
			}
        }
		return null;
	}

	public boolean containsPod(String _podUid) {
		boolean isContains = false;
		for (String key : resourceMap.keySet()) {
			ClusterWorkloadResource value = resourceMap.get(key);
			isContains = value.containsPod(_podUid);
			if(isContains == true) {
				return true;
			}
        }
		return false;
		//return mPods.containsKey(_podUid);
	}

	/**
	 * ClusterWorkloadResource 에 포함되어 있는 pod를 관리한다.
	 * @param _podUid
	 * @param _pod
	 */
	public boolean addPod(ClusterWorkloadPod _pod) {
		if(_pod.getOwnerName() == null) {
			return false;
		}
		
		ClusterWorkloadResource resource = this.resourceMap.get(_pod.getOwnerName());
		if(resource == null) {
			resource = new ClusterWorkloadResource();
			resource.setClUid(clUid);
			resource.setKind(_pod.getOwnerKind());
			resource.setNm(_pod.getOwnerName());
			resource.setUid(_pod.getOwnerUid());
		}
		
		resource.addPod(_pod);
		return true;
	}
//}} ClusterWorkloadResource[ClusterWorkloadPod] 관련
	
	/**
	 * 필요에 따라서 실행
	 */
	public void setResourcePodClUidAll() {
		if(this.clUid == null) {
			return;
		}
		
		for(Map.Entry<String, ClusterWorkloadResource> resourceE : this.resourceMap.entrySet() ) {
			resourceE.getValue().setClUid(this.clUid);
			Map<String, ClusterWorkloadPod> podMap = resourceE.getValue().getPodMap();
			for (Map.Entry<String, ClusterWorkloadPod> entry : podMap.entrySet()) {
				entry.getValue().setClUid(this.clUid);
			}
		}		
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
package com.kware.policy.task.collector.service.vo;

import java.util.HashMap;
import java.util.Map;

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
	private String info;
	private String memo;
	private String hashVal;
	private String mlId;
	private long   createdAt;
	private long   updatedAt;

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
	private Map<String, ClusterWorkloadPod> mPods = new HashMap<String, ClusterWorkloadPod>();

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

	
}
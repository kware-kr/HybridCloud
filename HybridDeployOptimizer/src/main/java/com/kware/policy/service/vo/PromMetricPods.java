package com.kware.policy.service.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 현재 수집 세션(시간)에 수집된 ProMetricPod 전체를 관리하기 위한 그룹 클래스 
 */
public class PromMetricPods extends PromMetricDefault{
	
	final static String key_spliter = "_";
	
	//key:clUid + _+ podUid
	Map<String, PromMetricPod> mPodList = new ConcurrentHashMap<String, PromMetricPod>();

	public void setMetricPod(PromMetricPod _value) {
		mPodList.put(_value.getClUid() + key_spliter + _value.getPodUid(), _value);
	}
	
	public PromMetricPod getMetricPod(Integer _clUid, String _podUid) {
		return mPodList.get(_clUid + key_spliter + _podUid);
	}
	
	/**
	 * 모든 노드의 리스트 제공
	 * @return
	 */
	public List<PromMetricPod> getAllPodList(){
		return new ArrayList<>(mPodList.values());
	}
	
	public List<PromMetricPod> getUnmodifiableAllPodList(){
		return Collections.unmodifiableList(new ArrayList<>(mPodList.values()));
	}
	
	/**
	 * 요청한 클래스터에 있는 파드 리스트 제공
	 * @param _clusterId
	 * @return
	 */
	public List<PromMetricPod> getPodListIfEquals(Integer _clUid){
		 List<PromMetricPod> resultList = mPodList.values().stream()
                .filter(node -> node.getClUid() == _clUid)
                .collect(Collectors.toList());
		
		return resultList;
	}
	
	/**
	 * 요청한 클래스터를 제외한 클러스터에 있는 파드 리스트 제공
	 * @param _clusterId
	 * @return
	 */
	public List<PromMetricPod> getPodListIfNotEquals(Integer _clUid){
		 List<PromMetricPod> resultList = mPodList.values().stream()
                .filter(node -> node.getClUid() != _clUid)
                .collect(Collectors.toList());
		
		return resultList;
	}
	
	/**
	 * 요청한 클래스터에  및 노드에 있는 파드 리스트 제공
	 * @param _clusterId
	 * @return
	 */
	public List<PromMetricPod> getPodListIfEquals(Integer _clUid, String _node  ){
		 List<PromMetricPod> resultList = mPodList.values().stream()
                .filter(p -> p.getClUid() == _clUid)
                .filter(p -> p.getNode().equals(_node))
                .collect(Collectors.toList());
		
		return resultList;
	}
	
	/**
	 * 요청한 클래스터 제외, 요청한 노드 제외한 노드에 있는 파드 리스트 제공
	 * @param _clusterId
	 * @return
	 */
	public List<PromMetricPod> getPodListIfNotEquals(Integer _clUid , String _node){
		 List<PromMetricPod> resultList = mPodList.values().stream()
                .filter(p -> p.getClUid() != _clUid)
                .filter(p -> !p.getNode().equals(_node))
                .collect(Collectors.toList());
		
		return resultList;
	}
	
	public void clear() {
		if(mPodList != null) {
			mPodList.forEach((key, value) -> value.clear());
			mPodList.clear();
		}
		mPodList = null;		
	}
}
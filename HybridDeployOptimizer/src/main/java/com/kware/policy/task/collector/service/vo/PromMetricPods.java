package com.kware.policy.task.collector.service.vo;

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
	Map<String, PromMetricPod> mPodMap = new ConcurrentHashMap<String, PromMetricPod>();

	public void setMetricPod(PromMetricPod _value) {
		mPodMap.put(_value.getClUid() + key_spliter + _value.getPodUid(), _value);
	}
	
	public PromMetricPod getMetricPod(Integer _clUid, String _podUid) {
		if(_clUid == null || _podUid == null)
			return null;
		return mPodMap.get(_clUid + key_spliter + _podUid);
	}
	
	public PromMetricPod getMetricPod(String _id) {
		return mPodMap.get(_id);
	}
	
	public Map<String, PromMetricPod> getPodsMap(){
		return this.mPodMap;
	}
	
	/**
	 * 모든 노드의 리스트 제공
	 * @return
	 */
	public List<PromMetricPod> getAllPodList(){
		return new ArrayList<>(mPodMap.values());
	}
	
	public List<PromMetricPod> getUnmodifiableAllPodList(){
		return Collections.unmodifiableList(new ArrayList<>(mPodMap.values()));
	}
	
	/**
	 * 요청한 클래스터에 있는 파드 리스트 제공
	 * @param _clusterId
	 * @return
	 */
	public List<PromMetricPod> getPodListIfEquals(Integer _clUid){
		 List<PromMetricPod> resultList = mPodMap.values().stream()
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
		 List<PromMetricPod> resultList = mPodMap.values().stream()
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
		 List<PromMetricPod> resultList = mPodMap.values().stream()
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
		 List<PromMetricPod> resultList = mPodMap.values().stream()
                .filter(p -> p.getClUid() != _clUid)
                .filter(p -> !p.getNode().equals(_node))
                .collect(Collectors.toList());
		
		return resultList;
	}
	
	public void clear() {
		if(mPodMap != null) {
			mPodMap.forEach((key, value) -> value.clear());
			mPodMap.clear();
		}
		mPodMap = null;		
	}
}
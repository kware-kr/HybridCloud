package com.kware.policy.task.collector.service.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 현재 수집 세션(시간)에 수집된 ProMetricNode 전체를 관리하기 위한 그룹 클래스 
 */
public class PromMetricNodes extends PromMetricDefault{
	//key:cluit + "_" + node
	Map<String, PromMetricNode> mNodeList = new ConcurrentHashMap<String, PromMetricNode>();
	
	public void setMetricNode(PromMetricNode _value) {
		mNodeList.put(_value.getClUid() + "_" + _value.getNode(), _value);
	}
	
	public PromMetricNode getMetricNode(Integer _clUid, String _node) {
		return mNodeList.get(_clUid + "_" + _node);
	}
	
	/**
	 * 모든 노드의 리스트 제공
	 * @return
	 */
	public List<PromMetricNode> getAllNodeList(){
		return new ArrayList<>(mNodeList.values());
	}
	
	public List<PromMetricNode> getUnmodifiableAllNodeList(){
		return Collections.unmodifiableList(new ArrayList<>(mNodeList.values()));
	}
	
	/**사용가능한 리스트 제공**/
	public List<PromMetricNode> getUnmodifiableAppliableNodeList(){
		List<PromMetricNode> list = mNodeList.values().stream()
                .filter(p -> p.canHandle())
                .collect(Collectors.toList());	
		return Collections.unmodifiableList(new ArrayList<>(list));
	}
	
	//간단한, cpu, gpu, memory, disk 요청량을 제공하고, 배포가능한 리스트를 제공함
	public List<PromMetricNode> getUnmodifiableAppliableNodeList(int _cpu, long _memory, long _disk, int _gpu){
		List<PromMetricNode> list = mNodeList.values().stream()
                .filter(p -> p.canHandle(_cpu, _memory, _disk, _gpu))
                .collect(Collectors.toList());		
		return Collections.unmodifiableList(new ArrayList<>(list));
	}
	
	/*
	public Map<String, PromMetricNode> getAllNodeMap(){
		return mNodeList;
	}
	*/
	
	/**
	 * 클래스터에 있는 노드 리스트 제공
	 * @param _clusterId
	 * @return
	 */
	public List<PromMetricNode> getNodeListIfEquals(Integer _clUid){
		 List<PromMetricNode> resultList = mNodeList.values().stream()
                .filter(node -> node.getClUid() == _clUid)
                .collect(Collectors.toList());
		
		return resultList;
	}
	
	public List<PromMetricNode> getNodeListIfNotEquals(Integer _clUid){
		 List<PromMetricNode> resultList = mNodeList.values().stream()
               .filter(node -> node.getClUid() != _clUid)
               .collect(Collectors.toList());
		
		return resultList;
	}
	
	public void clear() {
		if(mNodeList != null) {
			mNodeList.forEach((key, value) -> value.clear());
			mNodeList.clear();
		}
		mNodeList = null;		
	}
}
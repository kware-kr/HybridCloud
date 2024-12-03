package com.kware.policy.task.common.queue;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kware.policy.task.collector.service.vo.ClusterWorkloadResource;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Container;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.RequestContainerAttributes;
import com.kware.policy.task.selector.service.vo.WorkloadTaskContainerWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역으로 사용할 스레드에 안전한 큐와 맵을 생성함

not
요청에 응답했지만 prometheus metric에 발견되지 않을때 얼마동안 가지고 있다가 없엘지 확인
 */
@Slf4j
public class WorkloadContainerQueue  extends DefaultQueue{
	private static final Logger queueLog = LoggerFactory.getLogger("queue-log");
    
    private final ConcurrentHashMap<String/*cl_uid_node_name*/, ConcurrentHashMap<String/*mlid_container-name*/, WorkloadTaskContainerWrapper>> nodeMap;
    private final ConcurrentHashMap<String/*mlid*/, List<WorkloadTaskContainerWrapper>> mlIdMap;
    //}}요청관리
  
    public WorkloadContainerQueue() {
    	nodeMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, WorkloadTaskContainerWrapper>>();
    	mlIdMap = new ConcurrentHashMap<String/*mlId*/, List<WorkloadTaskContainerWrapper>>();
    }
    
    public void setWorkloadTaskContainer(WorkloadTaskContainerWrapper wrapper) {
    	String nodeKey = wrapper.getContainer().getNodeKey();
    	String containerKey = wrapper.getContainer().getContainerKey();
    	String mlidKey = wrapper.getContainer().getMlId();
    	
    	ConcurrentHashMap<String/*mlid_container-name*/, WorkloadTaskContainerWrapper> cmap = this.nodeMap.get(nodeKey);
    	if(cmap == null) {
    		cmap = new ConcurrentHashMap<String, WorkloadTaskContainerWrapper>();
    		this.nodeMap.put(nodeKey, cmap); //생성될때 한번만 입력하자, 변경은 값(객체) 변경은 자동 
    	}
    	cmap.put(containerKey, wrapper);
    	
    	List<WorkloadTaskContainerWrapper> wrapperList = this.mlIdMap.get(mlidKey);
    	if(wrapperList == null) {
    		wrapperList = new ArrayList<WorkloadTaskContainerWrapper>();
    		this.mlIdMap.put(mlidKey, wrapperList);
    	}
    	wrapperList.add(wrapper);
    	
    }
    
    public void setWorkloadTaskContainer(List<WorkloadTaskContainerWrapper> wrapperList) {
    	for(WorkloadTaskContainerWrapper w: wrapperList) {
    		this.setWorkloadTaskContainer(w);
    	}
    }
        
    public void removeWorkloadTaskContainer(String mlId) {
    	String nodeKey;
    	String containerKey;
    	String mlidKey = mlId;
    	
    	List<WorkloadTaskContainerWrapper> wrapperList = this.mlIdMap.remove(mlidKey);
    	if(wrapperList != null) {
    		for(WorkloadTaskContainerWrapper w: wrapperList) {
    			nodeKey = w.getContainer().getNodeKey();
    			containerKey = w.getContainer().getContainerKey();
    			this.nodeMap.get(nodeKey).remove(containerKey);
    		}
    		wrapperList.clear();
    	}
    }
    
    //컨테이너 리스트틀 통해서 worapper 클래스를 최초 생성한는 유틸리티성 함수
    public List<WorkloadTaskContainerWrapper> initWorkloadTaskWrapperList(List<Container> containers){
    	containers.sort(Comparator.comparingInt(c-> c.getAttribute().getOrder()));
    	Integer order = -1;
    	RequestContainerAttributes attr;
    	LocalDateTime now = LocalDateTime.now();
    	int minutesToAdd = 1; //최초 1분후에 배포 된다고 가정하여 초기값 설정
    	LocalDateTime startTime = now.plusMinutes(minutesToAdd);
    	LocalDateTime endTime = null;
    	
    	WorkloadTaskContainerWrapper wrapper;
    	List<WorkloadTaskContainerWrapper> wrapperList = new ArrayList<WorkloadTaskContainerWrapper>();
    	
    	for(Container c : containers) {
    		attr = c.getAttribute();
    		wrapper = new WorkloadTaskContainerWrapper(c);
    		if(attr.getOrder() > order) {
    			minutesToAdd = attr.getPredictedExecutionTime();
    			order = c.getAttribute().getOrder();
    			
    			endTime = startTime.plusMinutes(minutesToAdd);
    		}
    		
    		wrapper.setEstimatedStartTime(startTime);
    		wrapper.setEstimatedEndTime(endTime);
    		wrapperList.add(wrapper);
    	}
    	
    	return wrapperList;
    }
    
    
    /**
     * 해당 컨테이너가 작업 시작통지를 받을 때  이후의 작업에 대한 부분을 모두 변경한다.
     * @param t
     * @param containerKey
     */
    public void shiftEstimateTime_start(Timestamp t, String mlid, String containerKey/*mlid_contaienrname*/) {
    	LocalDateTime lt = t.toLocalDateTime();
    	
    	
    	
    }
    
    /**
     * 해당 컨테이너가 작업 완료 통지를 받을 때  이후의 작업에 대한 부분을 모두 변경한다.
     * @param t
     * @param containerKey
     */
    public void shiftEstimateTime_end(Timestamp t, String containerKey/*mlid_contaienrname*/) {
    	
    }
    
    
    /**
     * 실제 배포가 되면 나오는 정보를 설정하고, 기존 cluid와 node가 변경되었는지 확인하고 nodeMap을 수정한다.
     */
    public void setPodInfo(PromMetricPod pmPod, ClusterWorkloadResource cwResource) {
    	List<WorkloadTaskContainerWrapper> wrappers = this.mlIdMap.get(pmPod.getMlId());
    	
    	//리플리카셋이 설정된 경우에는 경우에는 컨테이너가
    	String resourceKind = cwResource.getKind();
    	if(resourceKind.endsWith("Workflow")) {
    		
    	}
    	
    	
    	String containerName = null;
    	String podName = pmPod.getPod();
    	for(WorkloadTaskContainerWrapper wrapper: wrappers) {
    		containerName = wrapper.getContainer().getName();
    		if(podName.contains(containerName)) {
    			
    		}
    		
    		
    	}
    }
    
    
    
    
    
    
    
    
    
    

    /**
     * 
     * @return
     */
  	public Map<String, ConcurrentHashMap<String, WorkloadTaskContainerWrapper>> getNodeMap() {
  		return this.nodeMap;
  	}
  	
  	public Map<String, List<WorkloadTaskContainerWrapper>> getmlIdMap() {
  		return this.mlIdMap;
  	}
  	
  	public List<WorkloadTaskContainerWrapper> getmlIdList(String mlId) {
  		return this.mlIdMap.get(mlId);
  	}
  	
	/**
	 * 
	 * @return
	 */
    public Map<String, Map<String,WorkloadTaskContainerWrapper>> getReadOnlyNodeMap() {
    	return Collections.unmodifiableMap(this.nodeMap);    	
    }	
  	
  	/**
  	 * RuntimeWorkloaTaskdMap size
  	 * @return
  	 */
  	public int getRuntimeWorkloaTaskdMapSize() {
  		return this.nodeMap.size();
  	}
}
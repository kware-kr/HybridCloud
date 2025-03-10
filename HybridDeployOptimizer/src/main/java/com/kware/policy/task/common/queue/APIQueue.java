package com.kware.policy.task.common.queue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kware.policy.task.collector.service.vo.Cluster;
import com.kware.policy.task.collector.service.vo.ClusterDefault;
import com.kware.policy.task.collector.service.vo.ClusterNode;
import com.kware.policy.task.collector.service.vo.ClusterWorkload;
import com.kware.policy.task.collector.service.vo.ClusterWorkloadPod;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역으로 사용할 스레드에 안전한 큐와 맵을 생성함
 * @param <T>
 */
@Slf4j
@SuppressWarnings("unchecked")
public class APIQueue extends DefaultQueue{
	private static final Logger queueLog = LoggerFactory.getLogger("queue-log");
 
    //{{API
    public static enum APIMapsName{	CLUSTER, NODE, WORKLOAD, WORKLOADPOD 	};
    //keyinfo=> 
    //  cluster    : cluid
    //, node       : cluid + "_" + name
    //, workload   : mlid
    //, workloadpod: pod uid
    private final Map<APIMapsName, ConcurrentHashMap<String, ? extends ClusterDefault>> apiMap;
    //API를 통해 수집한 결과 저장, 최신 정보 1개만 유지
    //}}
    

    public APIQueue() {
    	queueLog.info("Queue Log Start ====================================================="); //로그 파일 생성하는 목적
    	log.error("Error Log Start ====================================================="); //로그 파일 생성하는 목적

    	apiMap = new ConcurrentHashMap<>();
    }

    
    //{{ ///////////////////////// API /////////////////////////////////
    // ConcurrentHashMap 관련 메서드
    private ConcurrentHashMap<String, ?> getApiMap(APIMapsName name) {
        return apiMap.computeIfAbsent(name, k -> new ConcurrentHashMap<>());
    }

    
    public boolean isEmpty() {
    	return this.apiMap.isEmpty();
    }

  //---------------------------------------------------------------------------------------------------
	/**
	 * Cluster class를 관리하는 ConcurrentHashMap
	 * @return
	 */
	public Map<String, Cluster> getApiClusterMap() {
		return (ConcurrentHashMap<String, Cluster>) this.getApiMap(APIMapsName.CLUSTER);
	}

	/**
	 * ClusterNod class를 관리하는 ConcurrentHashMap
	 * @return
	 */
	public Map<String, ClusterNode> getApiClusterNodeMap() {
		return (ConcurrentHashMap<String, ClusterNode>) getApiMap(APIMapsName.NODE);
	}

	/**
	 * ClusterWorkload class를 관리하는 ConcurrentHashMap
	 * @return
	 */

	public Map<String, ClusterWorkload> getApiWorkloadMap() {
		return (ConcurrentHashMap<String, ClusterWorkload>) getApiMap(APIMapsName.WORKLOAD);
	}

	/**
	 * ClusterWorkloadPod class를 관리하는 ConcurrentHashMap
	 * @return
	 */
	public Map<String, ClusterWorkloadPod> getApiWorkloadPodMap() {
		return (ConcurrentHashMap<String, ClusterWorkloadPod>) getApiMap(APIMapsName.WORKLOADPOD);
	}
	  	 	
  	
    public <T extends ClusterDefault>  Map<String, T> getReadOnlyApiMap(APIMapsName name) {
  		Map<String, T> map = (Map<String, T>)apiMap.get(name);
  		if(map == null)
  			return null;
  		
  		return Collections.unmodifiableMap(map);
  	}
    
    public <T extends ClusterDefault>  T getObject(APIMapsName name, String id) {
  		Map<String, T> map = (Map<String, T>)apiMap.get(name);
  		if(map == null)
  			return null;
  		
  		return map.get(id);
  	}
    
  	public int getApiQueueSize(APIMapsName name) {
  		Map<String, ?> map = apiMap.get(name);
  		if(map == null)
  			return 0;
  		else return map.size();
  	}
  	
  	
  	public <T extends ClusterDefault>  void putApiMap(APIMapsName name, T obj) {
  		Map<String, T> map = (Map<String, T>)apiMap.get(name);
  		map.put((String)obj.getUniqueKey(), obj);
  	}
  	
  	public <T extends ClusterDefault>  void putApiMap(T obj) {
  		APIMapsName name = null;
  		if(obj instanceof Cluster) {
  			name = APIMapsName.CLUSTER;
  		}else if(obj instanceof ClusterNode) {
  			name = APIMapsName.NODE;
  		}else if(obj instanceof ClusterWorkload) {
  			name = APIMapsName.WORKLOAD;
  		}else if(obj instanceof ClusterWorkloadPod) {
  			name = APIMapsName.WORKLOADPOD;
  		}else {
  			return;
  		}
  		
  		putApiMap(name, obj);
  	}
  	
    
    public void clearApiMaps(APIMapsName name) {
        ConcurrentHashMap<String, ?> map = apiMap.remove(name);
        
        String key = null;
        Iterator<String> iterator = map.keySet().iterator();
        while (iterator.hasNext()) { // 큐 내부의 모든 요소를 순회하면서 제거
        	key = iterator.next();
        	
        	ClusterDefault value = (ClusterDefault)map.get(key);
        	value.clear();
        	
            iterator.remove();
        }        
    }
    

    public void removeAllMaps() {
    	Iterator<APIMapsName> iterator = apiMap.keySet().iterator();
        while (iterator.hasNext()) { // 큐 내부의 모든 요소를 순회하면서 제거
        	clearApiMaps(iterator.next());
        }
    }
    
    //API 입력중에서 현재세션에서 제공한 데이터가 아닌 데이터를 즉 이전 세션에서 생성된 데이터를 제거함
    @SuppressWarnings("rawtypes")
	public HashMap removeNotIfSessionId(APIMapsName name, String sessionId) {
    	ConcurrentHashMap<String, ?> map = apiMap.get(name);
    	
    	String key = null;
    	Iterator<String> iterator =  map.keySet().iterator();
    	HashMap<String, Object> removedMap = new HashMap<String, Object>();
    	while (iterator.hasNext()) {
    	    key = iterator.next();
    	    ClusterDefault value = (ClusterDefault)map.get(key);
    	    
    	    String temp = null;
    	    
    	    if(value instanceof ClusterDefault) {
    	    	temp =value.getSessionId();
    	    }
/*    	    
    	    if(value instanceof Cluster ) {
    	    	temp = ((Cluster)value).getSessionId();
    	    }else if(value instanceof ClusterNode ) {
    	    	temp = ((ClusterNode)value).getSessionId();
    	    }else if(value instanceof ClusterWorkload ) {
    	    	temp = ((ClusterWorkload)value).getSessionId();
    	    }else {
    	    	;
    	    }
    	    */
    	    // 여기에 조건을 넣어서 조건이 맞으면 제거합니다.
    	    if (!sessionId.equals(temp)) {
    	    	removedMap.put(key, map.get(key));
    	    	
    	    	value.clear();
    	        iterator.remove(); // 현재 엔트리를 안전하게 제거합니다.
    	    }
    	}
    	return removedMap;
    }
    //}}API END
}
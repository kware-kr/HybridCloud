package com.kware.policy.task.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.kware.common.util.JSONUtil;
import com.kware.policy.task.collector.service.vo.PromQL;

/**
 * PromQL테이블에는 프로메테우스에 질의할 쿼리가 포함되어 있음
 * 프로메테우스에 쿼리할 내용과 쿼리결과에서 필요한 내용을 추출할 jsonpath를 jsonb로 만들어져 있음.
 * 
 * 이 클래스는 쿼리의 id를 기반으로 ExtractPaht를 관리한다.
 * ExtractPath는 프로메테우스에 질의할 promQL과 extractpath(질의 결과에서 필요한 데이터를 추출할 jsonpath 기반의 jsonb)로 구성되어 있음
 * 
 * 예) 
 * promql: kube_node_info * on(node,ref_id) group_right(internal_ip) (kube_node_status_condition == 1)
 * extract_path: 
 * { 
 *   "key": "node_info"
 * , "node": "$.metric.node"
 * , "cl_uid": "$.metric.ref_id"
 * , "instance": "$.metric.internal_ip"
 * , "timestamp": "$.value[0]"
 * , "status_condition": "$.metric.condition:$.metric.status"
 * }
 * 
 * 각 데이터를 추출한 ExtractPath를 JsonPath를 사용하기 위한 key,value Map으로 변환
 * 
 * 각 prqlUid를 통해서 PromQL 쿼리 및 쿼리결과로부터 각 정보 추출을 위한 Jsonpath를 제공.
 */
public class PromQLManager {
    private static final PromQLManager instance = new PromQLManager();
    ConcurrentHashMap<Integer, ExtractPath> mExtractList;

    private PromQLManager() {
    	mExtractList = new ConcurrentHashMap<Integer, ExtractPath>();
    }

    public static PromQLManager getInstance() {
        return instance;
    }
    
    public void setExtractPath(PromQL _promql ) {
    	ExtractPath t = mExtractList.get(_promql.getPrqlUid());
    	if(t == null) {
    		//이거 주석달까?
    		t = new ExtractPath(_promql);
    		mExtractList.put(_promql.getPrqlUid(), t);
    	}else {
    		if(!t.isEqual(_promql)) {
	    		//기존 데이터 삭제
    			mExtractList.remove(_promql.getPrqlUid());
	    		t.clear();
	    		
	    		//신규로 데이터 입력
	    		t = new ExtractPath(_promql);
	    		mExtractList.put(_promql.getPrqlUid(), t);
    		}
    	}
    }
    
    public void removeExtractPath(Integer _prqlUid) {
    	mExtractList.remove(_prqlUid);
    }
    
    /**
     * readOnly로 제공함, 데이터를 제거하지 못하도록
     * @param _prqlUid
     * @return
     */
    public Map<String, Object> getExtractPathMap(Integer _prqlUid){
    	ExtractPath t = mExtractList.get(_prqlUid);
    	if(t != null)
    		return Collections.unmodifiableMap(t.getExtractMap()); //읽기전용맵
    	return new HashMap<String, Object>();
    }
    
    public PromQL getPromQL(Integer _prqlUid){
    	ExtractPath t = mExtractList.get(_prqlUid);
    	if(t != null)
    		return t.getPromQL();
    	return null;
    }


	private class ExtractPath{
    	final PromQL promql;
    	Map<String, Object> extractMap = null;
    	
    	private ExtractPath(PromQL _promql) {
    		this.promql = _promql;
    		
    		if(this.extractMap != null)
				this.extractMap.clear();
			this.extractMap = null;
			
			extractMap = JSONUtil.getMapFromJsonString(this.promql.getExtractPath());	
    	}
		
		public PromQL getPromQL() {
			return this.promql;
		}
		

		public Map<String, Object> getExtractMap() {
			return extractMap;
		}
		
		public boolean isEqual(PromQL _promql) {
			return this.promql.getXmin() == _promql.getXmin();
		}
		
		public void clear() {
			this.extractMap.clear();
			this.extractMap = null;
		}
	}
}
package com.kware.policy.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.kware.common.util.JSONUtil;
import com.kware.policy.service.vo.PromQL;

/**
 * 전체 PromQL테이블의 프로메테우스에 쿼리할 냉용과 쿼리결과는 json으로 오는데 이 데이터에서 어떤데이터를 가져올지를 결정한 jsonpath를 저장한 json데이터를 관리한다.
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
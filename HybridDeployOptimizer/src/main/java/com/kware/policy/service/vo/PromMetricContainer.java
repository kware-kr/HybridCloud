package com.kware.policy.service.vo;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ToString
@Slf4j
public class PromMetricContainer extends PromMetricDefault{
	private Integer clUid;
	//private String noUid;
	///////////////////////////////////////////////
	private Timestamp promTimestamp;//쿼리결과마다 존재하는 프로메테우스의 시간정보

	private String node;     //가독성  나중에 지울수 있음
	private String podUid;
	private String pod;      //pode명
	private String container;

	private Timestamp createdTimestamp;
	private Boolean running;
	private Boolean waiting;
	private Boolean terminated;
	private String  terminatedReason;
	
	public final static Map<String, Method> m_containerMethdMap = PromMetricContainer.getMethodMapper();
	
	private static Map<String, Method> getMethodMapper(){
		PromMetricContainer p = new PromMetricContainer();
		Class<?> c = p.getClass();

		// 클래스의 모든 메서드 가져오기
		/*
        Method[] methods = c.getDeclaredMethods();
        for (Method method : methods) { // 메서드 이름 출력
        	if(method.getName().startsWith("set"))
        	   	log.info("Method name: " + method.getName());
        }
        */
        
        Map<String, Method> mMap = new HashMap<String, Method>();
        try {       	
//        	mMap.put("cl_uid"                  , c.getMethod("setClUid"                   , String.class));
//        	mMap.put("node"                    , c.getMethod("setNode"                    , String.class));
//        	mMap.put("pod"                     , c.getMethod("setPod"                     , String.class));
//        	mMap.put("pod_uid"                 , c.getMethod("setPodUid"                  , String.class));
//        	mMap.put("container"               , c.getMethod("setContainer"               , String.class));
        	mMap.put("created_timestamp"       , c.getMethod("setCreatedTimestamp"        , Timestamp.class));
        	mMap.put("running"                 , c.getMethod("setRunning"                 , Boolean.class));
        	mMap.put("waiting"                 , c.getMethod("setWaiting"                 , Boolean.class));
        	mMap.put("terminated"              , c.getMethod("setTerminated"              , Boolean.class));
        	mMap.put("terminated_reason"       , c.getMethod("setTerminatedReason"        , String.class));
        	mMap.put("cl_uid"                  , c.getMethod("setClUid"                   , Integer.class));
		} catch (NoSuchMethodException e) {
			log.error("",e);
		} catch (SecurityException e) {
			log.error("",e);
		}
        
        return mMap;
	}
}
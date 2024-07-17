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
public class PromMetricPod extends PromMetricDefault{
	private String mlId; //API에서 가져온 정보와 매핑
	private Integer clUid;
	//private String noUid;
	
	private Timestamp promTimestamp; //프로메테우스가 수집한 시간
	private Timestamp collectDt;

	///////////////////////////////////////////////
	
	private Map<String, String> labels = new HashMap<String, String>();; //pod labels

	private String node; //가독성  나중에 지울수 있음
	private String instance; //가독성 나중에 지울수 있음
	private String pod; //pode명
	private String podUid;
	private String kind; //created_by_kind
	private String parent; //created_by_이름
	private String namespace; //created_by_이름
	private Timestamp createdTimestamp;
	private Timestamp scheduledTimestamp;
	private Timestamp completedTimestamp;
	
	private Integer usageCpu1m;
	private Integer usageCpu;
	private Long usageMemory;
	private Long usageMemory1m;
	private Long usageDiskWrites1m;
	private Long usageDiskReads1m;
	private Long usageDiskIo1m;
	private Long usageDiskIo;
	private Long usageNetworkTransmit1m;
	private Long usageNetworkTransmit;
	private Long usageNetworkReceive1m;
	private Long usageNetworkReceive;
	private Long usageNetworkIo1m;
	private Long usageNetworkIo;

	private Map<Integer, Integer> mUsgeGpuMap = new HashMap<Integer, Integer>(); //개별 GPU사용량:<GPU 번호, 사용량은 %>
	private Map<String, Long> mLimitsList   = new HashMap<String, Long>();   //리소스이름: <(cpu, memory, gpu, disk), 값>
	private Map<String, Long> mRequestsList = new HashMap<String, Long>(); //리소스이름: <(cpu, memory, gpu, disk), 값>

	private Map<String, PromMetricContainer> mContainerList  = new HashMap<String, PromMetricContainer>(); //pod에 포함된 container
	
	public void setRequests(String _val) {
		String[] vals = _val.split(":");
		try {
			mRequestsList.put(vals[0], Long.parseLong(vals[1]));
		}catch(Exception e) {
			log.error("setRequests error:{}", _val, e);
		}
	}
	public void setLimits(String _val) {
		String[] vals = _val.split(":");
		try {
			mLimitsList.put(vals[0], Long.parseLong(vals[1]));
		}catch(Exception e) {
			log.error("setLimits error:{}", _val, e);
		}
	}
	
	public void setUsageGpu(String _val) {
		String[] vals = _val.split(":");
		try {
			mUsgeGpuMap.put(Integer.parseInt(vals[0]), Integer.parseInt(vals[1]));
		}catch(Exception e) {
			log.error("setUsageGpu error:{}", _val, e);
		}
	}
	
	//기존 맵에 데이터를 입력
	public void addLabels(Map<String, String> _labels) {
		if(labels == null) labels = new HashMap<String, String>();
		labels.putAll(_labels);
	}
	
	public boolean isCompleted() {
		if(completedTimestamp != null) 
			return true;
		return false;
	}
	
	public void clear() {
		if(labels != null) labels.clear();
		labels = null;
		
		if(mUsgeGpuMap != null) mUsgeGpuMap.clear();
		mUsgeGpuMap = null;
		
		if(mLimitsList != null) mLimitsList.clear();
		mLimitsList = null;
		
		if(mRequestsList != null) mRequestsList.clear();
		mRequestsList = null;
		
		if(mContainerList != null) mContainerList.clear();
		mContainerList = null;
	}
	
	public final static Map<String, Method> m_podMethdMap = PromMetricPod.getMethodMapper();
	
	private static Map<String, Method> getMethodMapper(){
		PromMetricPod p = new PromMetricPod();
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
//        	mMap.put("ml_id"                   , c.getMethod("setMlId"                    , String.class));
//        	mMap.put("node"                    , c.getMethod("setNode"                    , String.class));
//        	mMap.put("instance"                , c.getMethod("setInstance"                , String.class));
//        	mMap.put("pod"                     , c.getMethod("setPod"                     , String.class));
//        	mMap.put("pod_uid"                 , c.getMethod("setPodUid"                  , String.class));
        	mMap.put("parent"                  , c.getMethod("setParent"                  , String.class));
        	mMap.put("labels"                  , c.getMethod("setLabels"                  , Map.class));
        	mMap.put("kind"                    , c.getMethod("setKind"                    , String.class));
        	mMap.put("namespace"               , c.getMethod("setNamespace"               , String.class));
        	mMap.put("created_timestamp"       , c.getMethod("setCreatedTimestamp"        , Timestamp.class));
        	mMap.put("scheduled_timestamp"     , c.getMethod("setScheduledTimestamp"      , Timestamp.class));
        	mMap.put("completed_timestamp"     , c.getMethod("setCompletedTimestamp"      , Timestamp.class));
        	mMap.put("usage_cpu"               , c.getMethod("setUsageCpu"                , Integer.class));
        	mMap.put("usage_cpu_1m"            , c.getMethod("setUsageCpu1m"              , Integer.class));
        	mMap.put("usage_memory"            , c.getMethod("setUsageMemory"             , Long.class));
        	mMap.put("usage_memory_1m"         , c.getMethod("setUsageMemory1m"           , Long.class));
        	mMap.put("usage_disk_io"           , c.getMethod("setUsageDiskIo"             , Long.class));
        	mMap.put("usage_disk_io_1m"        , c.getMethod("setUsageDiskIo1m"           , Long.class));
        	mMap.put("usage_disk_reads_1m"     , c.getMethod("setUsageDiskReads1m"        , Long.class));
        	mMap.put("usage_disk_writes_1m"    , c.getMethod("setUsageDiskWrites1m"       , Long.class));
        	mMap.put("usage_network_transmit"  , c.getMethod("setUsageNetworkTransmit"    , Long.class));
        	mMap.put("usage_network_transmit_1m", c.getMethod("setUsageNetworkTransmit1m" , Long.class));
        	mMap.put("usage_network_io"        , c.getMethod("setUsageNetworkIo"          , Long.class));
        	mMap.put("usage_network_io_1m"     , c.getMethod("setUsageNetworkIo1m"        , Long.class));
        	mMap.put("usage_network_receive"   , c.getMethod("setUsageNetworkReceive"     , Long.class));
        	mMap.put("usage_network_receive_1m", c.getMethod("setUsageNetworkReceive1m"   , Long.class));
        	mMap.put("requests"                , c.getMethod("setRequests"                , String.class));
        	mMap.put("limits"                  , c.getMethod("setLimits"                  , String.class));
        	mMap.put("usage_gpu"               , c.getMethod("setUsageGpu"                , String.class));
        	mMap.put("cl_uid"                  , c.getMethod("setClUid"                   , Integer.class));
		} catch (NoSuchMethodException e) {
			log.error("",e);
		} catch (SecurityException e) {
			log.error("",e);
		}
        
        return mMap;
	}
}
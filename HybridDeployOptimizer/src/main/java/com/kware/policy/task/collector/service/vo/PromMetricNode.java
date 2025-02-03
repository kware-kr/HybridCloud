package com.kware.policy.task.collector.service.vo;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kware.common.config.serializer.HumanReadableSizeSerializer;
import com.kware.common.config.serializer.JsonIgnoreDynamicSerializer;
import com.kware.policy.task.common.constant.StringConstant;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Getter
@Setter
//@ToString
@Slf4j
public class PromMetricNode extends PromMetricDefault{
	public static enum Condition_TYPE {
		DiskPressure,MemoryPressure,NetworkUnavailable,PIDPressure,Ready
	};

	private Integer  uid;     //DB에 저장된 key값
	//private Integer prqlUid; //메트릭을위해 만든 promQl 아이디 
	private Integer clUid;
	private String noUuid;   //API에 있는 정보를 설정
	private Boolean status;  //API에 있는 정보 설정

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp promTimestamp;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp collectDt;

	///////////////////////////////////////////////
	private String  node;
	//private String  instance;
	//private String  namespace;
	private Boolean unscheduable = false; //role master또는 plane은 배포하지 않는다.
	private String  kubeVer;
	private String  role;


	Integer capacityCpu     = 0;    //밀리코어 
	Integer capacityGpu     = 0;    //GPU 갯수
	@JsonSerialize(using = HumanReadableSizeSerializer.class)
	Long    capacityDisk    = 0L;   //디스크 byte용량
	@JsonSerialize(using = HumanReadableSizeSerializer.class)
	Long    capacityMemory  = 0L; //메모리 byte용량
	Integer capacityPods    = 0;   //Pods 갯수
	@JsonSerialize(using = HumanReadableSizeSerializer.class)
	Long    capacityMaxHzCpu= 0L;

	Integer availableCpu    = 0;   //밀리코어로서: 코어갯수 * 1000을 전체 사용량으로 표시할 수 있음 실제는 더블형이나 Integer 형으로 변환 계산
	Integer availableGpu    = 0;   //gpu 갯수
	@JsonSerialize(using = HumanReadableSizeSerializer.class)
	Long    availableDisk   = 0L;
	@JsonSerialize(using = HumanReadableSizeSerializer.class)
	Long    availableMemory = 0L;
	Integer availablePods   = 0;  //pods 갯수
	
	//20240720: 사용정보를 일단 수집: 사용처는 차후에
	Double  usageNetworkTransmit1m = 0.0;
	Double  usageNetworkReceive1m  = 0.0;
	Double  usageDiskWrite1m = 0.0;
	Double  usageDiskRead1m    = 0.0;
	
	
	//Double betFitScore = 0.0;
	

	//이건 맵이 아니면 좋겠는데..
	private Map<Condition_TYPE, Boolean> statusConditions = new HashMap<Condition_TYPE, Boolean>(); //DiskPressure,MemoryPressure,NetworkUnavailable,PIDPressure,Ready
	
	@JsonSerialize(using = JsonIgnoreDynamicSerializer.class) //필요에 따라서 처리함
	private Map<String, String>  labels = new HashMap<String, String>();
	
	private Set<String> taintEffects = new HashSet<String>();
	
	@JsonIgnore
	public String getKey() {
		return this.clUid + StringConstant.STR_UNDERBAR + this.node;
	}

    
	//{{setter 함수
	public void setTaintEffect(String _val) { //NoSchedule, 
		taintEffects.add(_val);  //NoSchedule, PreferNoSchedule, NoExecute등이 오는데 모두 스케줄링하지 말라는 것임
		this.unscheduable = true; 
	}
	
	public void setStatusCondition(String _val) {
		String[] vals = _val.split(":");
		try {
			statusConditions.put(Condition_TYPE.valueOf(vals[0]), vals[1].equals("true"));
		}catch(Exception e) {
			log.error("setStatusCondtion error:{}", _val, e);
		}
	}
	
	
	private String[] getGpuArrayVals(String _val) {
		String[] vals = _val.split(":");
		int len = vals.length;
		
		if(len < 2)
			return null;
		
		StringBuilder result = new StringBuilder();
        for (int i = 0; i < len - 1; i++) {
        	if (i != 0) result.append("_");
            result.append(vals[i]);
        }
        
        String[] rst = new String[2];
        rst[0] = result.toString();
        rst[1] = vals[len - 1];
		
        return rst;
	}
	
	public void setCapacityGpuMemory(String _val) {
		String[] vals = getGpuArrayVals(_val);
		try {
			String gpuId = vals[0];
			
			PromMetricNodeGPU gpu = mGpuList.get(gpuId);
			if(gpu == null) {
				gpu = new PromMetricNodeGPU(gpuId);
				mGpuList.put(gpuId, gpu);
			}
			gpu.setCapacityMemory(Long.parseLong(vals[1]));
		}catch(Exception e) {
			log.error("setStatusCondtion error:{}", _val, e);
		}
	}
	
	public void setGpuModel(String _val) {
		String[] vals = getGpuArrayVals(_val);
		try {
			String gpuId = vals[0];
			
			PromMetricNodeGPU gpu = mGpuList.get(gpuId);
			if(gpu == null) {
				gpu = new PromMetricNodeGPU(gpuId);
				mGpuList.put(gpuId, gpu);
			}
			gpu.setModel(vals[1]);
		}catch(Exception e) {
			log.error("setStatusCondtion error:{}", _val, e);
		}
	}
	
	public void setGpuTemp(String _val) {
		String[] vals = getGpuArrayVals(_val);
		try {
			String gpuId = vals[0];
			
			PromMetricNodeGPU gpu = mGpuList.get(gpuId);
			if(gpu == null) {
				gpu = new PromMetricNodeGPU(gpuId);
				mGpuList.put(gpuId, gpu);
			}
			gpu.setTemp(Integer.parseInt(vals[1]));
		}catch(Exception e) {
			log.error("setStatusCondtion error:{}", _val, e);
		}
	}
	
	public void setAvailableGpuMemory(String _val) {
		String[] vals = getGpuArrayVals(_val);
		try {
			String gpuId = vals[0];
			
			PromMetricNodeGPU gpu = mGpuList.get(gpuId);
			if(gpu == null) {
				gpu = new PromMetricNodeGPU(gpuId);
				mGpuList.put(gpuId, gpu);
			}
			gpu.setAvailableMemory(Long.parseLong(vals[1]));
		}catch(Exception e) {
			log.error("setStatusCondtion error:{}", _val, e);
		}
	}
	
	public void setIndividualGpuUsage(String _val) {
		String[] vals = getGpuArrayVals(_val);
		try {
			String gpuId = vals[0];
			
			PromMetricNodeGPU gpu = mGpuList.get(gpuId);
			if(gpu == null) {
				gpu = new PromMetricNodeGPU(gpuId);
				mGpuList.put(gpuId, gpu);
			}
			gpu.setUsage(Double.parseDouble(vals[1]));
		}catch(Exception e) {
			log.error("setStatusCondtion error:{}", _val, e);
		}
	}
	//}}setter 함수들
	
	//기존 맵에 데이터를 입력
	public void addLabels(Map<String, String> _labels) {
		if(labels == null) labels = new HashMap<String, String>();
		labels.putAll(_labels);
	}
	
	//해당 노드에서 실행중인 파드 리스트
	Map<String, PromMetricPod> mPodList = new HashMap<String, PromMetricPod>();
	
	//GPU 모델은 의미없지만 추후에 모델별로 순위를 고려해서
	Map<String, PromMetricNodeGPU> mGpuList = new HashMap<String, PromMetricNodeGPU>(); //gpu model
	
	
	/**
	 * 이노드가 배포 가능한지 확인, 단순히 문제가 있는지 확인
	 * @return boolean
	 */
	public boolean canHandle() {
        return !this.unscheduable &&
        	    statusConditions.getOrDefault(Condition_TYPE.Ready, true) && 
               !statusConditions.getOrDefault(Condition_TYPE.DiskPressure, false) &&
               !statusConditions.getOrDefault(Condition_TYPE.MemoryPressure, false) &&
               !statusConditions.getOrDefault(Condition_TYPE.PIDPressure, false) &&
               !statusConditions.getOrDefault(Condition_TYPE.NetworkUnavailable, false)
               ;
    }

	 /**
	  * 이 노드에 기본적으로 이 정도의 리소스롤 배포가능한가?
	  * 아직 배포가 안된 파드의 정보가 포함되지 않고, 현재 사용량 기준으로만 계산 
	  * @param _cpu
	  * @param _memory
	  * @param _disk
	  * @param _gpu
	  * @return
	  */
	public boolean canHandle(int _cpu, long _memory, long _disk, int _gpu) {
        return this.canHandle() &&
               availableCpu    >= _cpu &&
               availableMemory >= _memory &&
               availableDisk   >= _disk &&
               availableGpu    >= _gpu
               ;
    }
	
	
	public void clear() {
		if(labels != null) labels.clear();
		labels = null;
		
		if(statusConditions != null) statusConditions.clear();
		statusConditions = null;
		
		if(taintEffects != null) taintEffects.clear();
		taintEffects = null;
		
		if(mGpuList != null) mGpuList.clear();
		mGpuList = null;
		
		if(mPodList != null) {
			mPodList.forEach((key, value) -> value.clear());
			mPodList.clear();
		}
		mPodList = null;
	}
	
	public final static Map<String, Method> m_nodeMethodMap = PromMetricNode.getMethodMapper();
	
	private static Map<String, Method> getMethodMapper(){
		PromMetricNode p = new PromMetricNode();
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
//        	mMap.put("node"                , c.getMethod("setNode"              , String.class)); //공통
//        	mMap.put("instance"            , c.getMethod("setInstance"          , String.class));
//        	mMap.put("clUid"               , c.getMethod("setClUid"             , Integer.class));
        	mMap.put("noUuid"              , c.getMethod("setNoUuid"            , String.class));
        	mMap.put("role"                , c.getMethod("setRole"              , String.class));
        	mMap.put("labels"              , c.getMethod("setLabels"            , Map.class));
        	mMap.put("collect_dt"          , c.getMethod("setCollectDt"         , Timestamp.class));
        	mMap.put("unscheduable"        , c.getMethod("setUnscheduable"      , Boolean.class));
        	mMap.put("capacity_gpu"        , c.getMethod("setCapacityGpu"       , Integer.class));
        	mMap.put("capacity_cpu"        , c.getMethod("setCapacityCpu"       , Integer.class));
        	mMap.put("capacity_pods"       , c.getMethod("setCapacityPods"      , Integer.class));
        	mMap.put("capacity_disk"       , c.getMethod("setCapacityDisk"      , Long.class));
        	mMap.put("capacity_memory"     , c.getMethod("setCapacityMemory"    , Long.class));
        	mMap.put("available_gpu"       , c.getMethod("setAvailableGpu"      , Integer.class));
        	mMap.put("available_cpu"       , c.getMethod("setAvailableCpu"      , Integer.class));
        	mMap.put("available_pods"      , c.getMethod("setAvailablePods"     , Integer.class));
        	mMap.put("available_disk"      , c.getMethod("setAvailableDisk"     , Long.class));
        	mMap.put("available_memory"    , c.getMethod("setAvailableMemory"   , Long.class));
        	mMap.put("capacity_gpu_memory" , c.getMethod("setCapacityGpuMemory" , String.class));
        	mMap.put("available_gpu_memory", c.getMethod("setAvailableGpuMemory", String.class));
        	mMap.put("kube_ver"            , c.getMethod("setKubeVer"           , String.class));
        	mMap.put("taint_effect"        , c.getMethod("setTaintEffect"       , String.class));
        	mMap.put("status_condition"    , c.getMethod("setStatusCondition"   , String.class));
        	mMap.put("gpu_model"           , c.getMethod("setGpuModel"          , String.class));
        	mMap.put("gpu_temp"            , c.getMethod("setGpuTemp"           , String.class));
        	mMap.put("cl_uid"              , c.getMethod("setClUid"             , Integer.class));
        	mMap.put("usage_network_receive_1m"  , c.getMethod("setUsageNetworkReceive1m"  , Double.class));
        	mMap.put("usage_network_transmit_1m" , c.getMethod("setUsageNetworkTransmit1m" , Double.class));
        	mMap.put("usage_disk_read_1m"  , c.getMethod("setUsageDiskRead1m"   , Double.class));
        	mMap.put("usage_disk_write_1m" , c.getMethod("setUsageDiskWrite1m"  , Double.class));
        	mMap.put("capacity_maxhz_cpu"  , c.getMethod("setCapacityMaxHzCpu"  , Long.class));
        	mMap.put("individual_gpu_usage", c.getMethod("setIndividualGpuUsage", String.class));
        	
		} catch (NoSuchMethodException e) {
			log.error("",e);
		} catch (SecurityException e) {
			log.error("",e);
		}
        
        return mMap;     
	}

	@Override
	public String toString() {
		return "PromMetricNode [clUid=" + clUid + ", noUuid=" + noUuid + ", status=" + status + ", promTimestamp="
				+ promTimestamp + ", collectDt=" + collectDt + ", node=" + node + ", unscheduable=" + unscheduable
				+ ", kubeVer=" + kubeVer + ", role=" + role + ", capacityCpu=" + capacityCpu + ", capacityGpu="
				+ capacityGpu + ", capacityDisk=" + capacityDisk + ", capacityMemory=" + capacityMemory
				+ ", capacityPods=" + capacityPods + ", availableCpu=" + availableCpu + ", availableGpu=" + availableGpu
				+ ", availableDisk=" + availableDisk + ", availableMemory=" + availableMemory + ", availablePods="
				+ availablePods + ", usageNetworkTransmit1m=" + usageNetworkTransmit1m + ", usageNetworkReceive1m="
				+ usageNetworkReceive1m + ", usageDiskWrite1m=" + usageDiskWrite1m + ", usageDiskRead1m="
				+ usageDiskRead1m + ", statusConditions=" + statusConditions
				+ ", taintEffects=" + taintEffects + ", mPodList=" + mPodList + ", mGpuList=" + mGpuList + "]";
	}
}
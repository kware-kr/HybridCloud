package com.kware.policy.task.collector.service.vo;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


@Getter
@Setter
@ToString
@Slf4j
public class PromMetricNode extends PromMetricDefault{
	public static enum Condition_TYPE {
		DiskPressure,MemoryPressure,NetworkUnavailable,PIDPressure,Ready
	};

	//private String  uid;     //DB에 저장된 key값
	//private Integer prqlUid; //메트릭을위해 만든 promQl 아이디 
	private Integer clUid;
	private String  noUid;   //API에 있는 정보를 설정
	private Boolean status;  //API에 있는 정보 설정

//	private String metricKeys; //jsondata
//	private BigDecimal metricValue;
//	private Timestamp prqlDt;
	
	private Timestamp promTimestamp;
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
	Long    capacityDisk    = 0L;   //디스크 byte용량
	Long    capacityMemory  = 0L; //메모리 byte용량
	Integer capacityPods    = 0;   //Pods 갯수
	Long    capacityMaxHzCpu= 0L;

	Integer availableCpu    = 0;   //밀리코어로서: 코어갯수 * 1000을 전체 사용량으로 표시할 수 있음 실제는 더블형이나 Integer 형으로 변환 계산
	Integer availableGpu    = 0;   //gpu 갯수
	Long    availableDisk   = 0L;
	Long    availableMemory = 0L;
	Integer availablePods   = 0;  //pods 갯수
	
	//20240720: 사용정보를 일단 수집: 사용처는 차후에
	Double  usageNetworkTransmit1m = 0.0;
	Double  usageNetworkReceive1m  = 0.0;
	Double  usageDiskWrite1m = 0.0;
	Double  usageDiskRead1m    = 0.0;
	
	
	Double betFitScore = 0.0;
	

	//이건 맵이 아니면 좋겠는데..
	private Map<Condition_TYPE, Boolean> statusConditions = new HashMap<Condition_TYPE, Boolean>(); //DiskPressure,MemoryPressure,NetworkUnavailable,PIDPressure,Ready
	private Map<String, String>  labels = new HashMap<String, String>();
	private Set<String> taintEffects = new HashSet<String>();
	
	@JsonIgnore
	public String getKey() {
		return this.clUid + StringConstant.STR_UNDERBAR + this.node;
	}

	
	private static final int WEIGHT_CPU    = 1;
    private static final int WEIGHT_MEMORY = 1;
    private static final int WEIGHT_GPU    = 1;
    private static final int WEIGHT_DISK   = 1;
    
    private static final int WEIGHT_DISK_USAGE    = 1;
    private static final int WEIGHT_NETWORK_USAGE = 1;
    
    
	/**
     * 현재 노드의 스코어를 계산하여 반환
     * @param request 요청
     * @return double 노드의 스코어
     */
    @JsonIgnore
    public double getScore() {
    	return getScore(null, false);
    }
    
    /**
     * 리소스 요청을 반영한 스코어 계산
     * @param req
     * @param islimit
     * @return
     */
    public double getScore(WorkloadRequest req, boolean islimit) {
    	//쿠버네티스 bin packing 리소스 계산: https://kubernetes.io/ko/docs/concepts/scheduling-eviction/resource-bin-packing/
    	
        // 각 리소스에 대한 사용 비율을 계산
        //double cpuScore    = Math.abs((availableCpu - request.getTotalLimitCpu()) / (double) capacityCpu);
        //double memoryScore = Math.abs((availableMemory - request.getTotalLimitMemory()) / (double) capacityMemory);
        //double gpuScore    = Math.abs((availableGpu - request.getTotalLimitGpu()) / (double) capacityGpu);
        //double diskScore   = Math.abs((availableDisk - request.getTotalLimitDisk()) / (double) capacityDisk);
        
    	// 대략적인 정규화 min-max기준 
        //double cpuScore    = Math.abs(availableCpu);    //밀리코어
        //double gpuScore    = Math.abs(availableGpu) * 1000;  // 코어 * 1000
        //double memoryScore = Math.abs(availableMemory) / (1024 * 1024);  //MB
        //double diskScore   = Math.abs(availableDisk)  / (1024 * 1024) ;  //MB

     // 가중치를 적용한 스코어 계산
		/*   double weightedScore = WEIGHT_CPU    * cpuScore +
		                       WEIGHT_MEMORY * memoryScore +
		                       WEIGHT_GPU    * gpuScore +
		                       WEIGHT_DISK   * diskScore;*/
//    	if(this.unscheduable) return -1;

        double cpuRequest     = req == null ? 0.0: islimit == true? req.getTotalLimitCpu()   : req.getTotalRequestCpu();
        double memoryRequest  = req == null ? 0.0: islimit == true? req.getTotalLimitMemory(): req.getTotalRequestMemory();
        double gpuRequest     = req == null ? 0.0: islimit == true? req.getTotalLimitGpu()   : req.getTotalRequestGpu();
        double diskRequest    = req == null ? 0.0: islimit == true? req.getTotalLimitDisk()  : req.getTotalRequestDisk();
        double podRequest     = req == null ? 0.0: 1.0;
        /*
        double cpuScore    = getSingleScore(this.capacityCpu                  , this.availableCpu                   , cpuRequest   , WEIGHT_CPU);
        double memoryScore = getSingleScore(this.capacityMemory/ (1024 * 1024), this.availableMemory / (1024 * 1024), memoryRequest, WEIGHT_MEMORY);
        double diskScore   = getSingleScore(this.capacityDisk/ (1024 * 1024)  , this.availableDisk / (1024 * 1024)  , diskRequest  , WEIGHT_DISK);
        double gpuScore    = getSingleScore(this.capacityGpu/ (1024 * 1024)   , this.availableGpu / (1024 * 1024)   , gpuRequest   , WEIGHT_GPU);
        */
        double cpuScore    = getSingleScore(this.capacityCpu   , this.availableCpu   , cpuRequest   , WEIGHT_CPU);
        double memoryScore = getSingleScore(this.capacityMemory, this.availableMemory, memoryRequest, WEIGHT_MEMORY);
        double diskScore   = getSingleScore(this.capacityDisk  , this.availableDisk  , diskRequest  , WEIGHT_DISK);
        double gpuScore    = getSingleScore(this.capacityGpu   , this.availableGpu   , gpuRequest   , WEIGHT_GPU);
        double podScore    = getSingleScore(this.capacityPods  , this.availablePods  , podRequest   , 1);
        //int weightedSum = WEIGHT_CPU + WEIGHT_MEMORY + WEIGHT_GPU + WEIGHT_DISK;
        
        double weightedScore = (cpuScore + memoryScore + diskScore + gpuScore); 
        return weightedScore;
    }
    
    private double getSingleScore(double capacity, double avaiable, double request, int weight ) {
    	double totalrequest    = (capacity - avaiable) + request;
    	double usagePercentage = 100 - ((double)totalrequest / capacity) * 100;
    	int rawScore           = (int)Math.floor(usagePercentage / 10);
    	
    	return rawScore * weight;
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
	
	public void setCapacityGpuMemory(String _val) {
		String[] vals = _val.split(":");
		try {
			Integer gpuId = Integer.parseInt(vals[0]);
			
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
		String[] vals = _val.split(":");
		try {
			Integer gpuId = Integer.parseInt(vals[0]);
			
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
		String[] vals = _val.split(":");
		try {
			Integer gpuId = Integer.parseInt(vals[0]);
			
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
		String[] vals = _val.split(":");
		try {
			Integer gpuId = Integer.parseInt(vals[0]);
			
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
	//}}setter 함수들
	
	//기존 맵에 데이터를 입력
	public void addLabels(Map<String, String> _labels) {
		if(labels == null) labels = new HashMap<String, String>();
		labels.putAll(_labels);
	}
	
	//해당 노드에서 실행중인 파드 리스트
	Map<String, PromMetricPod> mPodList = new HashMap<String, PromMetricPod>();;
	
	//GPU 모델은 의미없지만 추후에 모델별로 순위를 고려해서
	Map<Integer, PromMetricNodeGPU> mGpuList = new HashMap<Integer, PromMetricNodeGPU>(); //gpu model
	
	
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
//        	mMap.put("noUid"               , c.getMethod("setNoUid"             , String.class));
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
		} catch (NoSuchMethodException e) {
			log.error("",e);
		} catch (SecurityException e) {
			log.error("",e);
		}
        
        return mMap;     
	}

	@Override
	public String toString() {
		return "PromMetricNode [clUid=" + clUid + ", noUid=" + noUid + ", status=" + status + ", promTimestamp="
				+ promTimestamp + ", collectDt=" + collectDt + ", node=" + node + ", unscheduable=" + unscheduable
				+ ", kubeVer=" + kubeVer + ", role=" + role + ", capacityCpu=" + capacityCpu + ", capacityGpu="
				+ capacityGpu + ", capacityDisk=" + capacityDisk + ", capacityMemory=" + capacityMemory
				+ ", capacityPods=" + capacityPods + ", availableCpu=" + availableCpu + ", availableGpu=" + availableGpu
				+ ", availableDisk=" + availableDisk + ", availableMemory=" + availableMemory + ", availablePods="
				+ availablePods + ", usageNetworkTransmit1m=" + usageNetworkTransmit1m + ", usageNetworkReceive1m="
				+ usageNetworkReceive1m + ", usageDiskWrite1m=" + usageDiskWrite1m + ", usageDiskRead1m="
				+ usageDiskRead1m + ", betFitScore=" + betFitScore + ", statusConditions=" + statusConditions
				+ ", taintEffects=" + taintEffects + ", mPodList=" + mPodList + ", mGpuList=" + mGpuList + "]";
	}
}
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kware.policy.task.scalor.worker;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kware.common.config.serializer.JsonIgnoreDynamicSerializer;
import com.kware.common.util.JSONUtil;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.collector.service.vo.PromMetricPods;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.WorkloadCommandManager;
import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.PromQueue.PromDequeName;
import com.kware.policy.task.common.queue.RequestQueue;
import com.kware.policy.task.common.queue.WorkloadContainerQueue;
import com.kware.policy.task.common.service.CommonService;
import com.kware.policy.task.common.vo.WorkloadCommand;
import com.kware.policy.task.feature.FeatureMain;
import com.kware.policy.task.feature.service.vo.PodScalingPolicy;
import com.kware.policy.task.feature.service.vo.PodScalingPolicy.TriggerType;
import com.kware.policy.task.scalor.service.vo.PodScalingInfo;
import com.kware.policy.task.selector.service.vo.WorkloadTaskWrapper;

/**
 * collectMain > collectWorker에서 수집후 입력된 큐에서 데이터를 take하면서 파싱한다.
 */

//@Slf4j
//@SuppressWarnings("rawtypes")
public class PodWorker extends Thread {
	private static final Logger log = LoggerFactory.getLogger("scale-log");
	
	boolean isTest = false;
	
	// QueueManager 인스턴스 가져오기
	QueueManager            qm = QueueManager.getInstance();
	WorkloadCommandManager wcm = WorkloadCommandManager.getInstance();
	
//	private PromQLService pqService;
	private CommonService comService;
	private FeatureMain   featureMain;
	
	boolean isRunning = false;
	
	//이미 전송한 파드를 관리하고, 현재는 1시간 또는 30분 : 나중에 관리가 필요하면 QueueManager를 통해서 관리할 수 도 있음
	private static Map<String, Long> sendedMap = new HashMap<String, Long>();
	private final long SENDED_KEEPING_TIME = 30*60*1000;
	
	
	PromQueue            promQ = qm.getPromQ();
	RequestQueue      requestQ = qm.getRequestQ();
	WorkloadContainerQueue wcQ = qm.getWorkloadContainerQ();
	APIQueue              apiQ = qm.getApiQ();
		
/*	
	public void setPromQLService(PromQLService pqService) {
		this.pqService = pqService;
	}
*/
	public void setCommonService(CommonService comService) {
		this.comService = comService;
	}
	
	public void setFeatureMain(FeatureMain featureMain) {
		this.featureMain = featureMain;
		
	}
	
	//{{ 계속되는 데이터를 확인하기 위함
	//                 클러스터       노드명, 
	private static long collect_miliseconds = 0;  //이전의 처리했던 시간 데이터 즉 테이터가 변경되지 않으면 처리하지 않기위한 판단.
	//}}

	@Override
	public void run() {
		isRunning = true;
		this.removeSendedOldEntries(); //재배포 요청한 즉 스케일링 요청한 파드 중 시간이 지난 파드 제거(요청이 이루어 지지 않는 경우)

		//{{가장 최신 노드 데이터 조회
		//PromQueue pq = qm.getPromQ();
		PromMetricPods pods = null;
		Object obj = promQ.getPromDequesFirstObject(PromDequeName.METRIC_PODINFO);
		if(obj != null ) {
			pods = (PromMetricPods)obj;
			//{{시간이 이전에 수행했던 자료인지 확인
			if(collect_miliseconds >= pods.getTimemillisecond()) {
				return;
			}else {
				collect_miliseconds = pods.getTimemillisecond();
			}
			//}}
		}else {
			return;
		}
		//}}
		
		//{{pod 스케일링 정책 정보 조회
		PodScalingPolicy psp = featureMain.getFeatureBase_podScalingPolicies();
		List<PodScalingPolicy.AdjustmentTrigger> trList = new ArrayList<PodScalingPolicy.AdjustmentTrigger>();
		if(psp.getCpu().getScalingAt())	   trList.add(psp.getCpu());
		if(psp.getMemory().getScalingAt()) trList.add(psp.getMemory());
		if(psp.getGpu().getScalingAt())    trList.add(psp.getGpu());
		if(psp.getDisk().getScalingAt())   trList.add(psp.getDisk());
		//}}
	
		Map<String, PodScalingInfo> podSIResultMap = new HashMap<String, PodScalingInfo>();
		
		BlockingDeque<PromMetricPods> blockingQ = promQ.getPromPodsDeque();
		
		for(PodScalingPolicy.AdjustmentTrigger tr: trList) {
			//1. 각 파드별로 지속시간(예:10) 동안의 데이터를 메모리에서 가져온다.
			Map<String, List<PromMetricPod>> historyMap = this.collectRecentPodMetrics(blockingQ, tr.getObservationPeriod(), tr.getType());		
			String key = null;
			PodScalingInfo psInfo = null;
			List<PromMetricPod> podList = null;
			/*
			if(log.isDebugEnabled()) {
				try {
					JsonIgnoreDynamicSerializer.setIgnoreDynamic(true);
					log.debug("\n\nhistoryMap:\n{}", JSONUtil.getJsonstringFromObject(historyMap));
					JsonIgnoreDynamicSerializer.setIgnoreDynamic(false);
				} catch (JsonProcessingException e) {}
			}
			*/
			//각 리소스 별로 설정된 리소스를 가지고 pod의 리소스 사용량을 확인하는 작업
			Iterator<Map.Entry<String, List<PromMetricPod>>> iterator = historyMap.entrySet().iterator();
			while(iterator.hasNext()){
				Map.Entry<String, List<PromMetricPod>> entry = iterator.next();
				key     = entry.getKey(); //cluid_poduid
				podList = entry.getValue();
				
				//파드의 유지 시간상 리스트 
				psInfo = podSIResultMap.get(key);
				int size = podList.size();
				PromMetricPod tPod   = podList.get(size - 1);
				if(psInfo == null) {
					psInfo = new PodScalingInfo(0.0, 0.0, 0.0, 0.0);
					psInfo.promMetricPod = tPod;  //pod 기본 속성을 저장
					//psInfo.ps_policy     = psp;	  //파드 스케일 기본정보 저장, 로그 용도				
					//podSIResultMap.put(key, psInfo);
				}
				
				if(tr.getType() == PodScalingPolicy.TriggerType.CPU){
					psInfo.pod_cpu_size = size;
				}else if(tr.getType() == PodScalingPolicy.TriggerType.MEMORY) {
					psInfo.pod_mem_size = size;
				}else if(tr.getType() == PodScalingPolicy.TriggerType.DISK) {
					psInfo.pod_disk_size = size;
				}else if(tr.getType() == PodScalingPolicy.TriggerType.GPU) {
					psInfo.pod_gpu_size = size;
				}
				
				//시간대상에서 실제 스케일링 대상이 되는지 확인
				if(checkPodUsageList(podList, tr, psInfo)) {
					podSIResultMap.put(key, psInfo);
					//podSIResultMap.remove(key);
				}
				if(podList != null)
					podList.clear();
				iterator.remove();  //historyMap clear
			}
		}	
		
		if(log.isDebugEnabled()) {
			try {
				JsonIgnoreDynamicSerializer.setIgnoreDynamic(true);
				log.debug("\n\nPod Scaling 여부 확인 대상 Map:\n{}", JSONUtil.getJsonstringFromObject(podSIResultMap));
				JsonIgnoreDynamicSerializer.setIgnoreDynamic(false);
			} catch (JsonProcessingException e) {}
		}
		
		
		//현재 여기는 스케일링 대상이되는 파드를 이용하여 스케일링 정책의 조정비율을 적용하여 연산 
		for(Map.Entry<String, PodScalingInfo> entry : podSIResultMap.entrySet()) {
			String tKey = entry.getKey();
			PodScalingInfo tPSInfo = entry.getValue();
			
			//2가지 이상일대만 하도록 해볼까
			int count = 0;
			if(tPSInfo.cpu_isHigh != null) count++;
			if(tPSInfo.mem_isHigh != null) count++;
			if(tPSInfo.gpu_isHigh != null) count++;
			if(tPSInfo.disk_isHigh != null) count++;
			
			if(count < 2)
				continue;
			
			//}}2가지 이상일대만 하도록 해볼까
			
			if(!this.isTest) {
				if(sendedMap.containsKey(tKey)) {
					continue;
				}
			}
			
			//기존 값의 평균값을 처리한다.
			tPSInfo.makeAverage();
			
			PromMetricPod pod = tPSInfo.promMetricPod;
			Map<String, Long> limMap = pod.getMLimitsList();
			Map<String, Long> reqMap = pod.getMRequestsList();
			
			Boolean isHigh = null;
			Long reqVal = null;
			Long limVal = null;
			Long re_reqVal = null;
			Long re_limVal = null;
			int limt_add_per = 20;
			for(PodScalingPolicy.AdjustmentTrigger tr: trList) {
				if(tr.getType() == PodScalingPolicy.TriggerType.CPU){
					isHigh = tPSInfo.cpu_isHigh;
					if(isHigh != null) {
						reqVal = reqMap.get(POD_CPU);
						limVal = limMap.get(POD_CPU);
						
						re_reqVal = (long)(tPSInfo.cpu_val + (tPSInfo.cpu_val * (tr.getAdjustmentRate()/100.0)));
						re_limVal = (long)(tPSInfo.cpu_max_val + (tPSInfo.cpu_max_val * ((tr.getAdjustmentRate() + limt_add_per)/100.0)));
						
						/*
						if(limVal < re_reqVal) {
							re_limVal = (long)(re_reqVal + (re_reqVal / tr.getAdjustmentRate()));
						}else {
							re_limVal = limVal;
						}
						*/
						
						tPSInfo.addNewRequestsValue(POD_CPU, re_reqVal);
						tPSInfo.addNewLimitsValue  (POD_CPU, re_limVal);
					}
				}else if(tr.getType() == PodScalingPolicy.TriggerType.MEMORY) {
					isHigh = tPSInfo.mem_isHigh;
					if(isHigh != null) {
						reqVal = reqMap.get(POD_MEMORY);
						limVal = limMap.get(POD_MEMORY);
						
						re_reqVal = (long)(tPSInfo.mem_val + (tPSInfo.mem_val * (tr.getAdjustmentRate()/100.0)));
						re_limVal = (long)(tPSInfo.mem_max_val + (tPSInfo.mem_max_val * ((tr.getAdjustmentRate() + limt_add_per)/100.0)));
						
						/*
						if(limVal < re_reqVal) {
							re_limVal = (long)(re_reqVal + (re_reqVal / tr.getAdjustmentRate()));
						}else {
							re_limVal = limVal;
						}
						*/
						
						tPSInfo.addNewRequestsValue(POD_MEMORY, re_reqVal);
						tPSInfo.addNewLimitsValue  (POD_MEMORY, re_limVal);
					}
				}else if(tr.getType() == PodScalingPolicy.TriggerType.DISK) {
					isHigh = tPSInfo.disk_isHigh;
					if(isHigh != null) {
						reqVal = reqMap.get(POD_DISK);
						limVal = limMap.get(POD_DISK);
						
						re_reqVal = (long)(tPSInfo.disk_val + (tPSInfo.disk_val * (tr.getAdjustmentRate()/100.0)));
						re_limVal = (long)(tPSInfo.disk_max_val + (tPSInfo.disk_max_val * ((tr.getAdjustmentRate() + limt_add_per)/100.0)));
						
						/*
						if(limVal < re_reqVal) {
							re_limVal = (long)(re_reqVal + (re_reqVal / tr.getAdjustmentRate()));
						}else {
							re_limVal = limVal;
						}
						*/
						
						tPSInfo.addNewRequestsValue(POD_DISK, re_reqVal);
						tPSInfo.addNewLimitsValue  (POD_DISK, re_limVal);
					}
				}else if(tr.getType() == PodScalingPolicy.TriggerType.GPU) {
					isHigh = tPSInfo.gpu_isHigh;
					if(isHigh != null) {
						reqVal = reqMap.get(POD_GPU);
						limVal = limMap.get(POD_GPU);
						
						re_reqVal = (long)Math.ceil(tPSInfo.gpu_val + (tPSInfo.gpu_val * (tr.getAdjustmentRate()/100.0)));
						//re_limVal = (long)(tPSInfo.gpu_max_val + (tPSInfo.gpu_max_val * ((tr.getAdjustmentRate() + limt_add_per)/100.0)));
						re_limVal = re_reqVal; //GPU는 동일하게 설정
						
						tPSInfo.addNewRequestsValue(POD_GPU, re_reqVal);
						tPSInfo.addNewLimitsValue  (POD_GPU, re_limVal);
					}					
				}
			}

			//조정요청을 보낸다.
			//한번 보내면 다시 일정 시간이 지난 이후에 보낼까?
//			this.sendPodScaling(tPSInfo);
			//if(!sendedMap.containsKey(tKey)) {
			if(tPSInfo.getNewRequestsMap() != null) {
				this.sendPodScaling(tPSInfo);
				sendedMap.put(tKey, System.currentTimeMillis());
			}
			//}
		}
		//여기까지 하면 조정대상이되는 type과 전체 합한 값이 있음
		trList.clear();
		podSIResultMap.clear();
		
	}
	
	/**
	 * 파드 스케일링 요청 함수
	 * @param psInfo
	 */
	private void sendPodScaling(PodScalingInfo psInfo) {
		WorkloadCommand<PodScalingInfo> command = null;
		command = new WorkloadCommand<PodScalingInfo>(WorkloadCommand.CMD_POD_SCALING, psInfo);
		
		PromMetricPod ppod = psInfo.promMetricPod;
		this.comService.createEvent("파드 스케일링 대상", "PodScaleT"
				, "파드의 리소스를 조정을 위한 재배포 대상."
				+ StringConstant.STR_lineFeed + "  워크로드: " + ppod.getMlId() 
				+ StringConstant.STR_lineFeed + ", 클러스터: " + ppod.getClUid() 
				+ StringConstant.STR_lineFeed + ", 노드: "    + ppod.getNode() 
				+ StringConstant.STR_lineFeed + ", 파드: "    + ppod.getPod()
				+ StringConstant.STR_lineFeed + ", 평균 사용량:"    + psInfo.getAvearageString()
				+ StringConstant.STR_lineFeed + ", old request: " + ppod.getMRequestsList().toString()
				+ StringConstant.STR_lineFeed + ", new request: " + psInfo.getNewRequestsMap().toString()
				+ StringConstant.STR_lineFeed + ", old limit: "   + ppod.getMLimitsList().toString()
				+ StringConstant.STR_lineFeed + ", new limit: "   + psInfo.getNewLimitsMap().toString()
		);
		
		if(log.isDebugEnabled()) {
			try {
				JsonIgnoreDynamicSerializer.setIgnoreDynamic(true);
				log.debug("\n\npod Scaling Send:\n{}", JSONUtil.getJsonstringFromObject(psInfo));
				JsonIgnoreDynamicSerializer.setIgnoreDynamic(false);
			} catch (JsonProcessingException e) {
			}
		}else if(log.isInfoEnabled()) {
			log.info("파드 스케일링 요청: info={}", psInfo);
		}
		
		WorkloadCommandManager.addCommand(command);
	}
	
	/*
	 "limitsList"  : {"cpu": 9000, "memory": 734003200, "nvidia_com_gpu": 1 ,"ephemeral-storage": xxxxx}
	 "requestsList": {"cpu": 7000, "memory": 524288000, "nvidia_com_gpu": 1 ,"ephemeral-storage": xxxxx}
	*/
	public static String POD_CPU    = "cpu";
	public static String POD_MEMORY = "memory";
	public static String POD_GPU    = "nvidia_com_gpu";
	public static String POD_DISK   = "ephemeral-storage";
	/**
	 * 각 값들의 합 및 %값의 합을 제공함
	 * 한개라도 서로 값의 isHigh값이 달라지면 대상에서 제거하기 위해 null로 설정함.
	 * @param podList
	 * @param tr
	 * @param psInfo
	 * @return
	 */
	private boolean checkPodUsageList(List<PromMetricPod> podList, PodScalingPolicy.AdjustmentTrigger tr, PodScalingInfo psInfo) {
		Map<String, Long> limMap = null, reqMap = null;
		ScalingValue sval = null;
		for(PromMetricPod pod : podList) {
			if(limMap == null)
				limMap  = pod.getMLimitsList();
			if(reqMap == null)
				reqMap = pod.getMRequestsList();
			
			if(limMap == null && reqMap == null)
				return false;
			
			if(limMap != null && limMap.isEmpty() && reqMap != null && reqMap.isEmpty())
				return false;
			
			Double  cValue = null; //현재값
			Long    lValue = null; //limits
			Long    rValue = null; //requests
			
			//낮은쪽은 request 높은쪽은 limit를 기준으로 하되,
			//동일하게 할려면 같은 값을 설정하면 된다.
			if(tr.getType() == PodScalingPolicy.TriggerType.CPU){
				cValue = (double)pod.getUsageCpu1m();
				lValue = limMap==null ? null: limMap.get(POD_CPU);
				rValue = reqMap==null ? null: reqMap.get(POD_CPU);
				sval = checkPodUsage(cValue, tr, lValue, rValue);
				
				if(sval != null) {
					if(psInfo.cpu_isHigh != null && psInfo.cpu_isHigh != sval.isHigh) {
						sval = null;
					}else {
						psInfo.cpu_isHigh = sval.isHigh;
						psInfo.cpu_per   += sval.valuePer;
						psInfo.cpu_val   += sval.value;
						psInfo.cpu_max_val = psInfo.cpu_max_val < sval.value ? sval.value: psInfo.cpu_max_val; 
					}
				}
			}else if(tr.getType() == PodScalingPolicy.TriggerType.MEMORY) {
				cValue = (double)pod.getUsageMemory();
				lValue = limMap==null ? null: limMap.get(POD_MEMORY);
				rValue = reqMap==null ? null: reqMap.get(POD_MEMORY);
				
				sval = checkPodUsage(cValue, tr, lValue, rValue);
				
				if(sval != null) {
					if(psInfo.mem_isHigh != null && psInfo.mem_isHigh != sval.isHigh) {
						sval = null;
					}else {
						psInfo.mem_isHigh = sval.isHigh;
						psInfo.mem_per   += sval.valuePer;
						psInfo.mem_val   += sval.value;
						psInfo.mem_max_val = psInfo.mem_max_val < sval.value ? sval.value: psInfo.mem_max_val;
					}
				}
			}else if(tr.getType() == PodScalingPolicy.TriggerType.DISK) {
				/*
				cValue = (double)pod.getUsageDiskWrites1m();
				lValue = limMap==null ? null: limMap.get(POD_MEMORY);
				rValue = reqMap==null ? null: reqMap.get(POD_MEMORY);
				sval = checkPodUsage(cValue, tr, lValue, rValue);
				if(sval != null) {
					if(psInfo.disk_isHigh != null && psInfo.disk_isHigh != sval.isHigh) {
						sval = null;
					}else {
						psInfo.disk_isHigh = sval.isHigh;
						psInfo.disk_per   += sval.valuePer;
						psInfo.disk_val   += sval.value;
					}
				}
				*/
				return false;
			}else if(tr.getType() == PodScalingPolicy.TriggerType.GPU) {
				Map<String, Double> a = pod.getMUsgeGpuMap();
				if(a == null || a.size() ==0 ) {
					continue;
				}
				//int gpuCnt = a.size();
				double sum = 0.0; //0.0~1.0
				for (String key : a.keySet()) {
				    sum += a.get(key);
				}
				
				//cValue = sum *100;
				cValue = sum;
				lValue = limMap==null ? null: limMap.get(POD_GPU);
				//lValue = lValue==null ? null: lValue * 100;
				rValue = reqMap==null ? null: reqMap.get(POD_GPU);
				//rValue = rValue==null ? null: rValue * 100;
				sval = checkPodUsage(cValue, tr, lValue, rValue);
				
				if(sval != null) {
					if(psInfo.gpu_isHigh != null && psInfo.gpu_isHigh != sval.isHigh) {
						sval = null;
					}else {
						psInfo.gpu_isHigh = sval.isHigh;
						psInfo.gpu_per   += sval.valuePer;
						psInfo.gpu_val   += sval.value;
						psInfo.gpu_max_val = psInfo.gpu_max_val < sval.value ? sval.value: psInfo.gpu_max_val; //큰 의미 없음 gpu는 동일하게
					}
				}
			}
			
			if(sval == null)
				return false;			
		}
		return true;
	}
	
	class ScalingValue{
		Double  valuePer;
		Boolean isHigh;
		Double  value;
	}
	
	
	private ScalingValue checkPodUsage(Double  curValue, PodScalingPolicy.AdjustmentTrigger tr, Long limitValue, Long requestValue) {
		ScalingValue sval = null;
		
		Double usage_value_per = null;		
		if(limitValue != null && requestValue != null) { //낮은 쪽은 request 기준으로 하고, 높은쪽은 limits를 기준
			usage_value_per = ((double)curValue / requestValue) * 100; //%
			if(usage_value_per < tr.getDownTrigger()) {
				sval = new ScalingValue();
				sval.isHigh = false; //낮음
				sval.valuePer = usage_value_per; 
				sval.value    = (double)curValue; 
			}
			
			usage_value_per = ((double)curValue / limitValue) * 100; //%
			if(usage_value_per > tr.getUpTrigger()) {
				sval = new ScalingValue();
				sval.isHigh = true; //높음
				sval.valuePer = usage_value_per;
				sval.value    = (double)curValue;
			}
		}else if(limitValue != null) {
			sval = new ScalingValue();
			usage_value_per = ((double)curValue / limitValue) * 100; //%
			if(usage_value_per < tr.getDownTrigger()) {
				sval.isHigh = false;
			}else if(usage_value_per > tr.getUpTrigger()) {
				sval.isHigh = true;
			} else {
				sval = null;
			}
			
			if(sval != null) {
				sval.valuePer = usage_value_per;
				sval.value    = (double)curValue;
			}
		}else if(requestValue != null) {
			sval = new ScalingValue();
			usage_value_per = ((double)curValue / requestValue) * 100; //%
			if(usage_value_per < tr.getDownTrigger()) {
				sval.isHigh = false;
			}else if(usage_value_per > tr.getUpTrigger()) {
				sval.isHigh = true;
			}else {
				sval = null;
			}
			if(sval != null) {
				sval.valuePer = usage_value_per;
				sval.value    = (double)curValue;
			}
		}
		
		return sval;
    }

	/*
	 * 최근 지속시간 동안의 데이터를 조회하기
	 * */
	 private Map<String, List<PromMetricPod>> collectRecentPodMetrics(BlockingDeque<PromMetricPods> blockingQ, int periodMinutes, TriggerType triggerType) {
		long periodSeconds = periodMinutes * 60000;
        Map<String, List<PromMetricPod>> podHistory = new HashMap<>();
        long currentTime = System.currentTimeMillis(); // 현재 시간
        
        String mlId;
        String podUid;
        List<WorkloadTaskWrapper> taskWrappers;
        
        if(log.isDebugEnabled())
        	log.debug("Q({}) size:{}================================================",triggerType, blockingQ.size() );
//        WorkloadTaskWrapper taskWrapper;

        //최신 데이터부터 확인 (뒤쪽부터 순회)
        //BlockingDeque를 입력할때 addFirst 함수를 통해서 데이터를 입력하므로 iterator를 적용하면 처음부터 찾아서 조회하면 된다.
        //                       addLast  함수를 적용하면 blockingQ.descendingIterator()를 사용하여야 한다.
        Iterator<PromMetricPods> iterator = blockingQ.iterator();
        HashSet<String> excludeSet = new HashSet<String>();
        
        while (iterator.hasNext()) {
            PromMetricPods podList = iterator.next();
            long timestamp = podList.getTimemillisecond(); // PromMetricPods가 가진 수집 시간
            
            // 시간분이 넘은 데이터는 탐색 종료
            long diff = currentTime - timestamp;
            if(diff > periodSeconds) {
            	if(log.isDebugEnabled()){
                	String timestampString = null;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
                    timestampString = sdf.format(new Date(timestamp));
                    log.debug("");
                	log.debug("history map({}): 지속시간:{}, 시간차:{}({}), 대상시간:{}, 현시간:{}"
                			, triggerType
                			, periodSeconds, diff,  (diff - periodSeconds)
                			, timestampString,  sdf.format(new Date(currentTime)) );
                }
                break;
            }
            // PromMetricPods 내부의 Pod Map 조회
            PromMetricPod podData = null;
            for (Map.Entry<String, PromMetricPod> entry : podList.getPodsMap().entrySet()) {
                String key = entry.getKey(); //cluid_podUid
                
                if(excludeSet.contains(key)) {
                	continue;
                }
                
                podData = entry.getValue();
                
                mlId   = podData.getMlId();
                if(mlId == null || this.requestQ.getWorkloadRequest(mlId) == null) {
                	continue;
                }
                
                //if(!"RUNNING".equals(podData.getStatusPhase().toString())){  //모든 상태가 running 상태일때만 처리
                if(StringConstant.PodStatusPhase.RUNNING != podData.getStatusPhase()) {
                	excludeSet.add(key);
                	List<PromMetricPod> rmData = podHistory.remove(key);
                	if(rmData != null)
                		rmData.clear();
                	continue;
                }
              
              //{{재배포여부 결정: checkpoint 여부에 따른 시간처리 70%, 30%
                podUid = podData.getPodUid();
                boolean isRedeploy = false;
                taskWrappers = this.wcQ.getWorkloadTaskWrapperList(mlId);
                if(taskWrappers != null) {
	                for(WorkloadTaskWrapper wr: taskWrappers) {
	                	if(podUid.equals(wr.getPodUid())){
	                		isRedeploy = shouldRedeploy(wr.getCheckpoint(), wr.getScheduledTimestamp(), wr.getEstimatedEndTime());
	                		break;
	                	}
	                }
                }
                //}}
              
                if(log.isDebugEnabled()) {
                	log.debug("mlId중 {}:{}:{}",isRedeploy, mlId, podData.getPod());
                }
                
                if(isRedeploy) {
	                // 해당 파드의 데이터 리스트 생성 또는 추가
	                podHistory.computeIfAbsent(key, k -> new ArrayList<>()).add(podData);
                }else{
                	excludeSet.add(key);
                	List<PromMetricPod> rmData = podHistory.remove(key);
                	if(rmData != null)
                		rmData.clear();
                	continue;
                }                
            }
        }
        
        podHistory.entrySet().removeIf(entry -> {
            int size = entry.getValue().size();
            if (size < periodMinutes) {
                log.debug("Removing key: {}, list size: {} ", entry.getKey(), size);
                return true;
            }else {
            	log.debug("Targeting key: {}, list size: {} ", entry.getKey(), size);
            }
            return false;
        });
        

        if(log.isDebugEnabled())
        	log.debug("Q({}) End================================================\n", triggerType);
        
        excludeSet.clear();
        return podHistory;
    }
	 
	/**
	 * 재배포 여부를 결정하는 메서드.
	 *
	 * @param hasCheckpoint   체크포인트 지원 여부 (true이면 체크포인트 있음)
	 * @param startTime       작업 시작 시간 (LocalDateTime)
	 * @param expectedEndTime 예상 작업 종료 시간 (LocalDateTime)
	 * @return 재배포 조건을 만족하면 true, 아니면 false
	 */
	public boolean shouldRedeploy(boolean hasCheckpoint, LocalDateTime startTime, LocalDateTime expectedEndTime) {
		// 현재 시간을 함수 내부에서 생성
		LocalDateTime currentTime = LocalDateTime.now();

		// 전체 예상 작업 시간(초) 계산
		long totalExpectedSeconds = Duration.between(startTime, expectedEndTime).getSeconds();
		// 경과 시간(초) 계산
		long elapsedSeconds = Duration.between(startTime, currentTime).getSeconds();
		
		// 시작 5분은 체크하지 않음 warm-up time
	    if (elapsedSeconds < 300) {
	        return false;
	    }
		
		// 진행률 (%) 계산
		double progressPercent = ((double) elapsedSeconds / totalExpectedSeconds) * 100.0;

		// 체크포인트가 있으면 70% 미만, 없으면 30% 미만이면 재배포 조건 충족
		double threshold = hasCheckpoint ? 70.0 : 30.0;
/*
		// 디버그용 출력 (필요에 따라 제거)
		System.out.println("Has Checkpoint: " + hasCheckpoint);
		System.out.println("Start Time: " + startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		System.out.println("Expected End Time: " + expectedEndTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		System.out.println("Current Time: " + currentTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		System.out.println("Elapsed Time (s): " + elapsedSeconds);
		System.out.println("Total Expected Time (s): " + totalExpectedSeconds);
		System.out.println("Current Progress (%): " + progressPercent);
		System.out.println("Threshold (%): " + threshold);
*/
		
		
		if(isTest)
			return true; //테스트
		else
			return progressPercent < threshold;
	}
	 
	 private void removeSendedOldEntries() {
        long currentTime = System.currentTimeMillis();

        // iterator를 사용하여 안전하게 제거
        Iterator<Map.Entry<String, Long>> iterator = sendedMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            // 현재 시간과 엔트리의 시간 차이가 한 시간 이상이면 제거
            if (currentTime - entry.getValue() > SENDED_KEEPING_TIME) {
                iterator.remove();
            }
        }
    }
	
}

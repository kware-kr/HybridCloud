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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;

import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.collector.service.vo.PromMetricPods;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.WorkloadCommandManager;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.PromQueue.PromDequeName;
import com.kware.policy.task.common.service.CommonService;
import com.kware.policy.task.common.vo.WorkloadCommand;
import com.kware.policy.task.feature.FeatureMain;
import com.kware.policy.task.feature.service.vo.PodScalingPolicy;
import com.kware.policy.task.scalor.service.vo.PodScalingInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * collectMain > collectWorker에서 수집후 입력된 큐에서 데이터를 take하면서 파싱한다.
 */

@Slf4j
@SuppressWarnings("rawtypes")
public class PodWorker extends Thread {
	// QueueManager 인스턴스 가져오기
	QueueManager            qm = QueueManager.getInstance();
	WorkloadCommandManager wcm = WorkloadCommandManager.getInstance();
	
//	private PromQLService pqService;
	private CommonService comService;
	private FeatureMain   featureMain;
	
	boolean isRunning = false;
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
//	private static Map<Integer, Map<String, List<NodeScalingInfo>>> cluster_high_nodes = new HashMap<Integer, Map<String, List<NodeScalingInfo>>>();
//	private static Map<Integer, Map<String, List<NodeScalingInfo>>> cluster_low_nodes  = new HashMap<Integer, Map<String, List<NodeScalingInfo>>>();
	private static long collect_miliseconds = 0;  //이전의 처리했던 시간 데이터 즉 테이터가 변경되지 않으면 처리하지 않기위한 판단.
	//}}

	@Override
	public void run() {
		isRunning = true;

		//{{가장 최신 노드 데이터 조회
		PromQueue pq = qm.getPromQ();
		PromMetricPods pods = null;
		Object obj = pq.getPromDequesFirstObject(PromDequeName.METRIC_PODINFO);
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
		
		BlockingDeque<PromMetricPods> blockingQ = pq.getPromPodsDeque();
		
		for(PodScalingPolicy.AdjustmentTrigger tr: trList) {
			Map<String, List<PromMetricPod>> historyMap = this.collectRecentPodMetrics(blockingQ, tr.getObservationPeriod());		
			String key = null;
			PodScalingInfo psInfo = null;
			List<PromMetricPod> podList = null;
			
			Iterator<Map.Entry<String, List<PromMetricPod>>> iterator = historyMap.entrySet().iterator();
			while(iterator.hasNext()){
				Map.Entry<String, List<PromMetricPod>> entry = iterator.next();
				key     = entry.getKey();
				podList = entry.getValue();
				
				//파드의 유지 시간상 리스트 
				psInfo = podSIResultMap.get(key);
				int size = podList.size();
				PromMetricPod tPod   = podList.get(size - 1);
				if(psInfo == null) {
					psInfo = new PodScalingInfo(0.0, 0.0, 0.0, 0.0);
					psInfo.promMetricPod = tPod;  //pod 기본 속성을 저장
					//psInfo.ps_policy     = psp;	  //파드 스케일 기본정보 저장, 로그 용도				
					podSIResultMap.put(key, psInfo);
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
				
				if(!checkPodUsageList(podList, tr, psInfo)) {
					podSIResultMap.remove(key);
				}
				iterator.remove();
			}
			historyMap.clear();
		}		
		
		//현재 여기는 스케일링 대상이되는 파드를 이용하여 스케일링 정책의 조정비율을 적용하여 연산 
		for(Map.Entry<String, PodScalingInfo> entry : podSIResultMap.entrySet()) {
			String tKey = entry.getKey();
			PodScalingInfo tPSInfo = entry.getValue();
			
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
			for(PodScalingPolicy.AdjustmentTrigger tr: trList) {
				if(tr.getType() == PodScalingPolicy.TriggerType.CPU){
					isHigh = tPSInfo.cpu_isHigh;
					reqVal = reqMap.get(POD_CPU);
					limVal = limMap.get(POD_CPU);
					if(isHigh != null) {
						re_reqVal = (long)(tPSInfo.cpu_val + (tPSInfo.cpu_val / tr.getAdjustmentRate()));
						
						if(limVal < re_reqVal) {
							re_limVal = (long)(re_reqVal + (re_reqVal / tr.getAdjustmentRate()));
						}else {
							re_limVal = limVal;
						}
						
						reqMap.put(POD_CPU, re_reqVal);
						limMap.put(POD_CPU, re_limVal);
					}
				}else if(tr.getType() == PodScalingPolicy.TriggerType.MEMORY) {
					isHigh = tPSInfo.mem_isHigh;
					reqVal = reqMap.get(POD_MEMORY);
					limVal = limMap.get(POD_MEMORY);
					if(isHigh != null) {
						re_reqVal = (long)(tPSInfo.mem_val + (tPSInfo.mem_val / tr.getAdjustmentRate()));
						if(limVal < re_reqVal) {
							re_limVal = (long)(re_reqVal + (re_reqVal / tr.getAdjustmentRate()));
						}else {
							re_limVal = limVal;
						}
						
						reqMap.put(POD_MEMORY, re_reqVal);
						limMap.put(POD_MEMORY, re_limVal);
					}
				}else if(tr.getType() == PodScalingPolicy.TriggerType.DISK) {
					isHigh = tPSInfo.disk_isHigh;
					reqVal = reqMap.get(POD_DISK);
					limVal = limMap.get(POD_DISK);
					if(isHigh != null) {
						re_reqVal = (long)(tPSInfo.disk_val + (tPSInfo.disk_val / tr.getAdjustmentRate()));
						if(limVal < re_reqVal) {
							re_limVal = (long)(re_reqVal + (re_reqVal / tr.getAdjustmentRate()));
						}else {
							re_limVal = limVal;
						}
						
						reqMap.put(POD_DISK, re_reqVal);
						limMap.put(POD_DISK, re_limVal);
					}
				}else if(tr.getType() == PodScalingPolicy.TriggerType.GPU) {
					isHigh = tPSInfo.gpu_isHigh;
					reqVal = reqMap.get(POD_GPU);
					limVal = limMap.get(POD_GPU);
					if(isHigh != null) { 
						re_reqVal = (long)Math.ceil(tPSInfo.gpu_val + (tPSInfo.gpu_val / tr.getAdjustmentRate()));
						re_limVal = re_reqVal; //GPU는 동일하게 설정
						
						reqMap.put(POD_GPU, re_reqVal);
						limMap.put(POD_GPU, re_limVal);
					}					
				}
			}

			//조정요청을 보낸다.
			this.sendPodScaling(tPSInfo);
			
		}
		//여기까지 하면 조정대상이되는 type과 전체 합한 값이 있음
		trList.clear();
		podSIResultMap.clear();
		
	}
	
	private void sendPodScaling(PodScalingInfo psInfo) {
		WorkloadCommand<PodScalingInfo> command = null;
		command = new WorkloadCommand<PodScalingInfo>(WorkloadCommand.CMD_POD_SCALING, psInfo);
		
		PromMetricPod ppod = psInfo.promMetricPod;
		this.comService.createEvent("파드 스케일링 요청", "PodScale"
				, "파드의 리소스를 조정을 위한 재배포 요청.\n" 
				+ "클러스터 아이디:" + ppod.getClUid() 
				+ ", 노드 아이디:"  + ppod.getNode() 
				+ ", 파드 아이디:"  + ppod.getPod()
				+ ", 파드 request:"+ ppod.getMRequestsList().toString()
				+ ", 파드 limit:"  + ppod.getMLimitsList().toString()
				);
		
		log.info("파드 스케일링 요청: info={}", psInfo);
		
		WorkloadCommandManager.addCommand(command);
	}
	
	/*
	 "limitsList"  : {"cpu": 9000, "memory": 734003200, "nvidia_com_gpu": 1 ,"ephemeral-storage": xxxxx}
	 "requestsList": {"cpu": 7000, "memory": 524288000, "nvidia_com_gpu": 1 ,"ephemeral-storage": xxxxx}
	*/
	static String POD_CPU    = "cpu";
	static String POD_MEMORY = "memory";
	static String POD_DISK   = "nvidia_com_gpu";
	static String POD_GPU    = "ephemeral-storage";
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
			
			Double  cValue = null; //현재값
			Long    lValue = null; //limits
			Long    rValue = null;   //requests
			
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
				
				cValue = sum * 100;
				lValue = limMap==null ? null: limMap.get(POD_GPU);
				lValue = lValue==null ? null: lValue * 100;
				rValue = reqMap==null ? null: reqMap.get(POD_GPU);
				rValue = rValue==null ? null: rValue * 100;
				sval = checkPodUsage(cValue, tr, lValue, rValue);
				
				if(sval != null) {
					if(psInfo.gpu_isHigh != null && psInfo.gpu_isHigh != sval.isHigh) {
						sval = null;
					}else {
						psInfo.gpu_isHigh = sval.isHigh;
						psInfo.gpu_per   += sval.valuePer;
						psInfo.gpu_val   += sval.value;
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
		ScalingValue sval = new ScalingValue();
		
		Double usage_value_per = null;		
		if(limitValue != null && requestValue != null) { //낮은 쪽은 request 기준으로 하고, 높은쪽은 limits를 기준
			usage_value_per = ((double)curValue / requestValue) * 100; //%
			if(usage_value_per < tr.getDownTrigger()) {
				sval.isHigh = false; //낮음
				sval.valuePer = usage_value_per; 
				sval.value    = (double)curValue; 
			}
			
			usage_value_per = ((double)curValue / limitValue) * 100; //%
			if(usage_value_per > tr.getUpTrigger()) {
				sval.isHigh = true; //높음
				sval.valuePer = usage_value_per;
				sval.value    = (double)curValue;
			}
		}else if(limitValue != null) {
			usage_value_per = ((double)curValue / limitValue) * 100; //%
			if(usage_value_per < tr.getDownTrigger()) {
				sval.isHigh = false;
			}else if(usage_value_per > tr.getUpTrigger()) {
				sval.isHigh = true;
			}
			sval.valuePer = usage_value_per;
			sval.value    = (double)curValue;
		}else if(requestValue != null) {
			usage_value_per = ((double)curValue / requestValue) * 100; //%
			if(usage_value_per < tr.getDownTrigger()) {
				sval.isHigh = false;
			}else if(usage_value_per > tr.getUpTrigger()) {
				sval.isHigh = true;
			}
			sval.valuePer = usage_value_per;
			sval.value    = (double)curValue;
		}
		
		return sval;
    }

	/*
	 * 최근 지속시간 동안의 데이터를 조회하기
	 * */
	 public Map<String, List<PromMetricPod>> collectRecentPodMetrics(BlockingDeque<PromMetricPods> blockingQ, int periodMinutes) {
		long periodSeconds = periodMinutes * 60000L;
        Map<String, List<PromMetricPod>> podHistory = new HashMap<>();
        long currentTime = System.currentTimeMillis(); // 현재 시간

        //최신 데이터부터 확인 (뒤쪽부터 순회)
        //BlockingDeque를 입력할때 addFirst 함수를 통해서 데이터를 입력하므로 iterator를 적용하면 처음부터 찾아서 조회하면 된다.
        //                       addLast  함수를 적용하면 blockingQ.descendingIterator()를 사용하여야 한다.
        Iterator<PromMetricPods> iterator = blockingQ.iterator();
        while (iterator.hasNext()) {
            PromMetricPods podList = iterator.next();
            long timestamp = podList.getTimemillisecond(); // PromMetricPods가 가진 수집 시간

            // 시간분이 넘은 데이터는 탐색 종료
            if (currentTime - timestamp > periodSeconds) {
                break;
            }

            // PromMetricPods 내부의 Pod Map 조회
            for (Map.Entry<String, PromMetricPod> entry : podList.getPodsMap().entrySet()) {
                String key = entry.getKey();
                PromMetricPod podData = entry.getValue();
                if(!"RUNNING".equals(podData.getStatusPhase().toString())){  //running 상태일때만 처리
                	continue;
                }

                // 해당 파드의 데이터 리스트 생성 또는 추가
                podHistory.computeIfAbsent(key, k -> new ArrayList<>()).add(podData);
            }
        }
        return podHistory;
    }
	
}

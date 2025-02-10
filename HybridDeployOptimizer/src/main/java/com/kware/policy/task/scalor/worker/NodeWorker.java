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

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.collector.service.vo.PromMetricNodes;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.WorkloadCommandManager;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.PromQueue.PromDequeName;
import com.kware.policy.task.common.service.CommonService;
import com.kware.policy.task.common.vo.WorkloadCommand;
import com.kware.policy.task.feature.FeatureMain;
import com.kware.policy.task.feature.service.vo.NodeScalingPolicy;
import com.kware.policy.task.scalor.service.vo.NodeScalingInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * collectMain > collectWorker에서 수집후 입력된 큐에서 데이터를 take하면서 파싱한다.
 */

@Slf4j
public class NodeWorker extends Thread {
	//private static final Logger log = LoggerFactory.getLogger("debug-log");
	
	// QueueManager 인스턴스 가져오기
	QueueManager            qm = QueueManager.getInstance();
	WorkloadCommandManager wcm = WorkloadCommandManager.getInstance();
	
//	private PromQLService pqService;
	private CommonService comService;
	private FeatureMain   featureMain;
	
	private final static int NODE_SCALE_CHECK_MINUTE =10;
	
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
	//                 클러스터       노드명,  n개의 시간동안의 값
	private static Map<Integer, Map<String, List<NodeScalingInfo>>> cluster_high_nodes = new HashMap<Integer, Map<String, List<NodeScalingInfo>>>();
	private static Map<Integer, Map<String, List<NodeScalingInfo>>> cluster_low_nodes  = new HashMap<Integer, Map<String, List<NodeScalingInfo>>>();
	private static long collect_miliseconds = 0;  //이전의 처리했던 시간 데이터 즉 테이터가 변경되지 않으면 처리하지 않기위한 판단.
	//}}
	
	class NodePairs {
		Integer clUid;
		List<String> nodeNames = new ArrayList<String>();
	}

	@Override
	public void run() {
		isRunning = true;
		
		//node 스케일링 정책 정보 조회
		NodeScalingPolicy nsp = featureMain.getFeatureBase_nodeScalingPolicies();
		if(!nsp.isScalingAt()){
			return;
		}
				
		//{{가장 최신 노드 데이터 조회
		PromQueue pq = qm.getPromQ();
		PromMetricNodes nodes = null;
		Object obj = pq.getPromDequesFirstObject(PromDequeName.METRIC_NODEINFO);
		if(obj != null ) {
			nodes = (PromMetricNodes)obj;
			//{{시간이 이전에 수행했던 자료인지 확인
			if(collect_miliseconds >= nodes.getTimemillisecond()) {
				return;
			}else {
				collect_miliseconds = nodes.getTimemillisecond();
			}
			//}}
		}else {
			return;
		}
		//}}	
		
		//{{스케일링을 진행하는데 클러스터가 없어지거나, 노드가 없어질경우 이 데이터를 제거해야함 
		Map<Integer, NodePairs> clusterNodePairMap = new HashMap<Integer, NodePairs>();
		for(PromMetricNode n : nodes.getUnmodifiableAllNodeList()) {
			if(n.getUnscheduable()) //배포불가능한 노드는 제외
				continue;

			NodePairs pairs = clusterNodePairMap.get(n.getClUid());
			if(pairs == null) {
				pairs = new NodePairs();
				pairs.clUid = n.getClUid();
				pairs.nodeNames.add(n.getNode());
			}
			clusterNodePairMap.put(n.getClUid(), pairs);
		}
		
		for(Map.Entry<Integer, NodePairs> p : clusterNodePairMap.entrySet()) {
			 Map<String, List<NodeScalingInfo>> amap = null;
			 amap = cluster_high_nodes.get(p.getKey());
			 if(amap != null)
				 amap.keySet().retainAll(p.getValue().nodeNames);
			 
			 amap = cluster_low_nodes.get(p.getKey());
			 if(amap != null)
				 amap.keySet().retainAll(p.getValue().nodeNames);
		}
		
		//정적변수이므로 기존 데이터를 계속 유지하고 있으며로, 노드기 사라지거나, 클러스터가 사라지면 제거해주어야 한다.
		cluster_high_nodes.keySet().retainAll(clusterNodePairMap.keySet());
		cluster_low_nodes.keySet().retainAll(clusterNodePairMap.keySet());
		clusterNodePairMap.clear();
		//}}
				
		//{{ 클러스터별 노드 카운트 확인
		Map<Integer, Integer> clusterNodeCntMap = new HashMap<Integer, Integer>();
		for(PromMetricNode n : nodes.getUnmodifiableAllNodeList()) {
			/*
			if(n.getUnscheduable()) //배포불가능한 노드는 제외
				continue;
			
			Integer cnt = clusterNodeCntMap.get(n.getClUid());
			if(cnt == null) {
				cnt = 0;
			}
			clusterNodeCntMap.put(n.getClUid(), ++cnt);
			*/
			if (n.getUnscheduable()) continue;
		    clusterNodeCntMap.merge(n.getClUid(), 1, Integer::sum);
		}
		//}}
		
		// 오류 클러스터에서 현재 정책적용이 되는 노드가 몇개인가(High가 몇개이고, 로그 몇개인가?를 확인해서 처리해야하는데 지금은 문제가 있음)
		NodeScalingInfo snode = null;
		for(PromMetricNode n : nodes.getUnmodifiableAllNodeList()) {
			if(n.getUnscheduable()) //배포불가능한 노드는 제외
				continue;
			
			snode = this.checkNodeUsage(n, nsp);
			snode.cur_node = n;
			if(snode.isHigh != null) {
				if(snode.isHigh) {
					setHighLow(n.getClUid(), n.getNode(), snode, cluster_high_nodes);
					removeHighLow(n.getClUid(), n.getNode(), cluster_low_nodes); //낮은 쪽을 제거
				}else {
					setHighLow(n.getClUid(), n.getNode(), snode, cluster_low_nodes);
					removeHighLow(n.getClUid(), n.getNode(), cluster_high_nodes); //높은 쪽을 제거
				}

			}else {
				//포함되지 않으면 모두 제거
				removeHighLow(n.getClUid(), n.getNode());
			}		
		}
		
		//스케일링 요청처리함
		sendNodeScaling(clusterNodeCntMap, nsp);
		
		if(log.isDebugEnabled()) {
			/*
			try {
				JsonIgnoreDynamicSerializer.setIgnoreDynamic(true);
				log.debug("\n\ncluster_high_nodes:\n{}", JSONUtil.getJsonstringFromObject(cluster_high_nodes));
				log.debug("cluster_low_nodes:\n{}\n\n", JSONUtil.getJsonstringFromObject(cluster_low_nodes));
				JsonIgnoreDynamicSerializer.setIgnoreDynamic(false);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			*/
			
			log.debug("\n\ncluster_high_nodes:\n{}", cluster_high_nodes);
			log.debug("cluster_low_nodes:\n{}\n\n", cluster_low_nodes);
		}
		
		clusterNodeCntMap.clear();
	}
	
	//스케일링 요청함, 이벤트 처리해야하
	private void sendNodeScaling(Map<Integer, Integer> clusterNodeCntMap, NodeScalingPolicy nsp ) {
		//{{남아있는 데이터가 있으면 노드스케일링 요청
		//keti 함수 호출, 호출 요청 하면 관련정보 제거하고, 관련내용을 로그 및 DB에 남겨야한다.
		//해당 클러스터와 관련된 정보를 모두 제거해야하나? 일단 10분간 대기하는 개념으로 모두 제거하자
		
		
		Integer clUid = null;
		int count = 0;
		int cur_node_cnt = 0;
		
		WorkloadCommand<List<NodeScalingInfo>> command = null; //하이 노드의 노드의 이름 전송
		List<NodeScalingInfo> nodScalingInfos = null;
		Map<String, List<NodeScalingInfo>> cluster_nodes = null;
		List<String> nodeNameList = new ArrayList<String>();

		List<Integer> removeClUidList = new ArrayList<Integer>();
		Iterator<Integer> iter = cluster_high_nodes.keySet().iterator();
		while(iter.hasNext()) {
			clUid = iter.next();
			cluster_nodes = cluster_high_nodes.get(clUid);
			if(cluster_nodes.isEmpty()) {
				iter.remove();
				continue;
			}
			
			cur_node_cnt = clusterNodeCntMap.get(clUid);  //클러스터의 현재 노드 갯수
			//count = cluster_nodes.size(); //클러스터내의 지속되는 고부하 노드의 갯수		
			for(Map.Entry<String, List<NodeScalingInfo>> entry : cluster_nodes.entrySet()) {
				//10분이상 지속된 자료가 있는지 확인
				if(entry.getValue().size() > NODE_SCALE_CHECK_MINUTE) {
					count++;
					nodeNameList.add(entry.getKey());
				}
			}
			String[] node_names = nodeNameList.toArray(new String[0]);
			nodeNameList.clear();
			
			if(count == 0)
				continue;

			//1개를 추가 요청하면
			if((cur_node_cnt + 1) <= nsp.getMaxCount()) { //현재 노드 갯수가  정책 최대값 보다 더 작으면 키우는 요청
				if(nsp.isScalingAt()) {
					nodScalingInfos = calculateAverage(cluster_nodes);
					command = new WorkloadCommand<List<NodeScalingInfo>>(WorkloadCommand.CMD_NODE_SCALING_OUT, nodScalingInfos);
					WorkloadCommandManager.addCommand(command);
				}
				
				this.comService.createEvent("노드 스케일 아웃[OUT] 판단", "NodeScale"
						, "클러스터의 고부하 노드로 인한 스케일 요청.\n클러스터 아이디:" + clUid 
						+ ", 노드:" + String.join(",", node_names)
				        + ", API 요청 정책:" + nsp.isScalingAt());
				
				if(log.isInfoEnabled())
					log.info("노드 스케일링 OUT 판단: cluster={},  고부하 nodes={}, API 요청 정책: {}",  clUid, nodScalingInfos, nsp.isScalingAt());				
			}
			removeClUidList.add(clUid); //요청을 보내면 삭제함
		}
		
		count = 0;
		iter = cluster_low_nodes.keySet().iterator();
		while(iter.hasNext()) {
			clUid = iter.next();
			cluster_nodes = cluster_low_nodes.get(clUid);
			if(cluster_nodes.isEmpty()) {
				iter.remove();
				continue;
			}
			
			cur_node_cnt = clusterNodeCntMap.get(clUid);  //클러스터의 현재 노드 갯수
			//count = cluster_nodes.size(); //클러스터내의 지속되는 고부하 노드의 갯수		
			for(Map.Entry<String, List<NodeScalingInfo>> entry : cluster_nodes.entrySet()) {
				//10분이상 지속된 자료가 있는지 확인
				if(entry.getValue().size() > NODE_SCALE_CHECK_MINUTE) {
					count++;
					nodeNameList.add(entry.getKey());
				}
			}
			String[] node_names = nodeNameList.toArray(new String[0]);
			nodeNameList.clear();
			
			if(count == 0)
				continue;
			
			//전체 노드 카운트에서 현재카운트를 뺐을때 
			if((cur_node_cnt - count) >= nsp.getMinCount()) { //현재 노드 갯수가 정책 최소값 보다 더 크면 줄이는 요청
				if(nsp.isScalingAt()) {
					nodScalingInfos = calculateAverage(cluster_nodes);
					command = new WorkloadCommand<List<NodeScalingInfo>>(WorkloadCommand.CMD_NODE_SCALING_IN, nodScalingInfos);
					WorkloadCommandManager.addCommand(command);
				}
				
				this.comService.createEvent("노드 스케일 인[IN] 판단", "NodeScale"
						, "클러스터의 여유 노드로 인한 스케일 요청.\n클러스터 아이디:" + clUid 
						+ ", 노드:" + String.join(",", node_names)
						+ ", API 요청 정책:" + nsp.isScalingAt());
				if(log.isInfoEnabled())
					log.info("노드 스케일링 IN 판단: cluster={}, nodes={}, API 요청 정책: {}",  clUid, nodScalingInfos, nsp.isScalingAt());
			}
			removeClUidList.add(clUid);
		}
		
		for(Integer i : removeClUidList) {
			removeHighLow(i);
		}
		removeClUidList.clear();
		
		//}}
		
	}
	
    /**
     * 각 클러스터(노드 리스트)에 대해, 속성별로 단 한 번의 반복문으로 합계와 null 여부를 체크한 후 평균을 계산합니다.
     * 각 속성에 대해, 하나라도 null이면 해당 속성의 평균은 null로 처리합니다.
     *
     * @param clusterNodes 클러스터별 NodeScalingInfo 리스트를 담은 맵
     * @return 각 클러스터별 평균값을 담은 NodeScalingInfo 객체들의 리스트
     */
    public List<NodeScalingInfo> calculateAverage(Map<String, List<NodeScalingInfo>> clusterNodes) {
        List<NodeScalingInfo> resultList = new ArrayList<>();

        // 각 클러스터(노드 리스트)에 대해 처리
        for (List<NodeScalingInfo> nodeList : clusterNodes.values()) {
            int count = nodeList.size();
            double sumCpu = 0.0, sumMem = 0.0, sumDisk = 0.0, sumGpu = 0.0;
            boolean cpuHasNull = false, memHasNull = false, diskHasNull = false, gpuHasNull = false;

            // 각 노드를 한 번씩 순회하면서 모든 속성의 합과 null 여부를 체크
            for (NodeScalingInfo node : nodeList) {
                // CPU 처리
                if (node.cpu_per == null) {
                    cpuHasNull = true;
                } else {
                    sumCpu += node.cpu_per;
                }

                // Memory 처리
                if (node.mem_per == null) {
                    memHasNull = true;
                } else {
                    sumMem += node.mem_per;
                }

                // Disk 처리
                if (node.disk_per == null) {
                    diskHasNull = true;
                } else {
                    sumDisk += node.disk_per;
                }

                // GPU 처리
                if (node.gpu_per == null) {
                    gpuHasNull = true;
                } else {
                    sumGpu += node.gpu_per;
                }
            }

            // 평균 계산: 하나라도 null이면 해당 속성은 null, 그렇지 않으면 합계/노드 수
            NodeScalingInfo avgNode = new NodeScalingInfo();
            avgNode.cpu_per  = cpuHasNull  ? null : (sumCpu  / count);
            avgNode.mem_per  = memHasNull  ? null : (sumMem  / count);
            avgNode.disk_per = diskHasNull ? null : (sumDisk / count);
            avgNode.gpu_per  = gpuHasNull  ? null : (sumGpu  / count);

            // 모든 노드의 cur_node 값이 동일하다는 가정 하에 첫 번째 노드의 값을 사용
            avgNode.cur_node = nodeList.get(0).cur_node;

            resultList.add(avgNode);
        }
        return resultList;
    }

	
	/**
	 * 클러스터를 스토리지에서 모두(상,하)에서 제거
	 * @param clUid
	 * @param nodeName
	 * @param snode
	 */
	private void removeHighLow(Integer clUid) {
		removeHighLow(clUid, cluster_high_nodes);
		removeHighLow(clUid, cluster_low_nodes);
	}
	
	private void removeHighLow(Integer clUid, Map<Integer, Map<String, List<NodeScalingInfo>>> cluster_nodes) {
		cluster_nodes.remove(clUid);
	}
	
	/**
	 * 현재 노드의 상태가 상,하 상태가 아니면, 기존에 지속되는 정보를 저장하는 스토리지에서 모두(상,하)에서 제거
	 * @param clUid
	 * @param nodeName
	 * @param snode
	 */
	private void removeHighLow(Integer clUid, String nodeName) {
		removeHighLow(clUid, nodeName, cluster_high_nodes);
		removeHighLow(clUid, nodeName, cluster_low_nodes);
	}
	
	private void removeHighLow(Integer clUid, String nodeName, Map<Integer, Map<String, List<NodeScalingInfo>>> cluster_nodes) {
		Map<String, List<NodeScalingInfo>> snodesMap = cluster_nodes.get(clUid);
		if(snodesMap != null) {		
			snodesMap.remove(nodeName);
		}
	}
	
	
	/**
	 * 평가가 완료된 노드를 각 해당 컬렉션 저장소에 등록하는 함수 
	 * @param clUid
	 * @param nodeName
	 * @param snode
	 */
	private void setHighLow(Integer clUid, String nodeName, NodeScalingInfo snode , Map<Integer, Map<String, List<NodeScalingInfo>>> cluster_nodes) {
		Map<String, List<NodeScalingInfo>> nodesMap = cluster_nodes.get(clUid);
		if(nodesMap == null) {
			nodesMap = new HashMap<String, List<NodeScalingInfo>>();
			cluster_nodes.put(clUid, nodesMap);
		}
		
		List<NodeScalingInfo> snodes = nodesMap.get(nodeName);
		if(snodes == null) {
			snodes = new ArrayList<NodeScalingInfo>();
		}
		snodes.add(snode);
		
		nodesMap.put(nodeName, snodes);
	}
		
	/**
	 * 노드의 상태(상,하)에 포함되는지 평가하는 함수
	 * @param curNode
	 * @param nsp
	 * @return
	 */
	private NodeScalingInfo checkNodeUsage(PromMetricNode curNode, NodeScalingPolicy nsp) {
		NodeScalingInfo snode = new NodeScalingInfo();
		NodeScalingPolicy.ScalingTrigger tr = null;
		
		Boolean cpu_isHigh, mem_isHigh, disk_isHigh, gpu_isHigh;
		cpu_isHigh=mem_isHigh=disk_isHigh=gpu_isHigh=null;
		
		List<Boolean> rstList = new ArrayList<Boolean>();
		
		//cpu----------------------------------------------------------
		tr = nsp.getCpu();
		if(tr.getScalingAt()) {
			int capacity_cpu   = curNode.getCapacityCpu();
			int usage_cpu      = capacity_cpu - curNode.getAvailableCpu();
			
			int inTrigger_cpu  = tr.getInTrigger();
			int outTrigger_cpu = tr.getOutTrigger();
			
			double usage_cpu_per =  capacity_cpu > 0 ? ((double) usage_cpu / capacity_cpu) * 100.0 : 0.0;
			if(usage_cpu_per < inTrigger_cpu) {
				cpu_isHigh = false;
			}else if(usage_cpu_per < outTrigger_cpu) {
				cpu_isHigh = true;
			}
			snode.cpu_per = usage_cpu_per;
			
			rstList.add(cpu_isHigh); //true, false, null
		}
		
		//mem----------------------------------------------------------
		tr = nsp.getMemory();
		if(tr.getScalingAt()) {
			long capacity_mem   = curNode.getCapacityMemory();
			long usage_mem      = capacity_mem - curNode.getAvailableMemory();
			
			int inTrigger_mem  = tr.getInTrigger();
			int outTrigger_mem = tr.getOutTrigger();
			
			double usage_mem_per = capacity_mem > 0 ? ((double)usage_mem/capacity_mem) * 100.0 : 0.0;
			if(usage_mem_per < inTrigger_mem) {
				mem_isHigh = false;
			}else if(usage_mem_per < outTrigger_mem) {
				mem_isHigh = true;
			}
			snode.mem_per = usage_mem_per;
			
			rstList.add(mem_isHigh); //true, false, null
		}
		
		//disk----------------------------------------------------------
		tr = nsp.getDisk();
		if(tr.getScalingAt()) {
			long capacity_disk   = curNode.getCapacityDisk();
			long usage_disk      = capacity_disk - curNode.getAvailableDisk();
			int inTrigger_disk  = tr.getInTrigger();
			int outTrigger_disk = tr.getOutTrigger();
			
			double usage_disk_per = capacity_disk > 0 ? ((double)usage_disk/capacity_disk) * 100.0 : 0.0;
			if(usage_disk_per < inTrigger_disk) {
				disk_isHigh = false;
			}else if(usage_disk_per < outTrigger_disk) {
				disk_isHigh = true;
			}
			snode.disk_per = usage_disk_per;
			
			rstList.add(disk_isHigh); //true, false, null
		}
		
		//gpu----------------------------------------------------------
		tr = nsp.getGpu();
		if(tr.getScalingAt()) {
			int capacity_gpu   = curNode.getCapacityGpu();
			if(capacity_gpu != 0) {
				int usage_gpu      = capacity_gpu - curNode.getAvailableGpu();
				int inTrigger_gpu  = tr.getInTrigger();
				int outTrigger_gpu = tr.getOutTrigger();
	
				double usage_gpu_per = capacity_gpu > 0 ? ((double)usage_gpu/capacity_gpu) * 100.0 : 0.0;
				if(usage_gpu_per < inTrigger_gpu) {
					gpu_isHigh = false;
				}else if(usage_gpu_per < outTrigger_gpu) {
					gpu_isHigh = true;
				}
				
				snode.gpu_per = usage_gpu_per;
				rstList.add(gpu_isHigh); //true, false, null
			}
		}
		
		Boolean evalConition = null;
		if("AND".equals(nsp.getScalingLogic())){
			evalConition = evaluateAndCondition(rstList);
		}else { //OR
			evalConition = evaluateOrCondition(rstList);
		}
		rstList.clear();
		
		snode.isHigh = evalConition;
		
		return snode;
    }
	
	
	/**
     * AND 조건 평가:
     * - 리스트 내에서 null이 아닌 값들을 모은 후,
     *   - 모두 true이면 true 반환
     *   - 모두 false이면 false 반환
     *   - 혼합되어 있거나 non-null 값이 하나도 없으면 null 반환
     */
    private Boolean evaluateAndCondition(List<Boolean> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        
     // 리스트에 null 값이 있으면 결과를 명확히 판단할 수 없으므로 null 반환
        if (list.contains(null)) {
            return null;
        }
        
        boolean allTrue  = list.stream().allMatch(b -> b);
        boolean allFalse = list.stream().allMatch(b -> !b);
        
        if (allTrue) {
            return true;
        } else if (allFalse) {
            return false;
        } else {
            // true와 false가 섞여 있는 경우
            return null;
        }
    }
    
    /**
     * OR 조건 평가:
     * - 리스트 내에서 하나라도 true가 있으면 true 반환
     * - 리스트가 null이거나 empty면 false 반환
     */
    private Boolean evaluateOrCondition(List<Boolean> list) {
    	if (list == null || list.isEmpty()) {
            return null;
        }

        boolean foundFalse = false;
        for (Boolean value : list) {
            if (Boolean.TRUE.equals(value)) {
                // 하나라도 true가 있으면 바로 true 반환
                return true;
            } else if (Boolean.FALSE.equals(value)) {
            	//false는 마지막까지 돌면서 처리해야한다. true가 언제 나올지 모르기 때문
                foundFalse = true;
            }
        }
        return foundFalse ? false : null;
    }

    
    
    
    
    
    private RealMatrix coefficients; // VAR(1) 계수 행렬
    
    /**
     * 다변량 시계열 데이터를 기반으로 VAR 모델을 이용하여 다음 시점의 사용량을 예측한다.
     *
     * @param resourceHistory 각 리소스별 과거 사용률 데이터를 담은 배열 (예: resourceHistory[0] = CPU, resourceHistory[1] = Memory, ...)
     * @return 예측된 사용량 배열 [CPU, Memory, Disk, GPU]
     */
    private double[] predictNextUsageMultivariate(List<Double>[] resourceHistory) {
       
    	/* 샘플
		@SuppressWarnings("unchecked")
        List<Double>[] resourceHistory = new List[4];
        resourceHistory[0] = cpuSeries;
        resourceHistory[1] = memorySeries;
        resourceHistory[2] = diskSeries;
        resourceHistory[3] = gpuSeries;

        NodeWorkerVARSample sample = new NodeWorkerVARSample();
        double[] predicted = sample.predictNextUsageMultivariate(resourceHistory);

        System.out.println("예측 결과 (최근 30개 관측값 기준):");
        System.out.println("CPU 사용률 예측: " + predicted[0] + "%");
        System.out.println("Memory 사용률 예측: " + predicted[1] + "%");
        System.out.println("Disk 사용률 예측: " + predicted[2] + "%");
        System.out.println("GPU 사용률 예측: " + predicted[3] + "%");
    	*/
    	
    	
        this.fit(resourceHistory);
        return this.forecast(1);
    }

    /**
     * 다변량 시계열 데이터를 이용하여 VAR(1) 모델을 학습.
     *
     * @param timeSeries 각 시계열 데이터 List 배열, 각 List의 길이는 30 이상이어야 함
     */
    private void fit(List<Double>[] timeSeries) {
        int d = timeSeries.length;        // 시계열의 개수 (차원)
        int T = timeSeries[0].size();       // 각 시계열의 길이
        if (T < 2) {
            throw new IllegalArgumentException("시계열 길이는 최소 2 이상이어야 합니다.");
        }

        // X1: d x (T-1) 행렬, X2: d x (T-1) 행렬
        double[][] X1Data = new double[d][T - 1];
        double[][] X2Data = new double[d][T - 1];

        for (int i = 0; i < d; i++) {
            List<Double> series = timeSeries[i];
            for (int t = 0; t < T - 1; t++) {
                X1Data[i][t] = series.get(t);       // t 시점
                X2Data[i][t] = series.get(t + 1);     // t+1 시점
            }
        }

        RealMatrix X1 = MatrixUtils.createRealMatrix(X1Data);
        RealMatrix X2 = MatrixUtils.createRealMatrix(X2Data);

        // VAR(1) 모델: X2 = A * X1 + error
        // 추정된 A = X2 * X1^+ (여기서 X1^+는 X1의 의사역행렬)
        SingularValueDecomposition svd = new SingularValueDecomposition(X1);
        RealMatrix X1PseudoInverse = svd.getSolver().getInverse();

        coefficients = X2.multiply(X1PseudoInverse);
    }

    /**
     * VAR(1) 모델을 이용해 다음 시점의 사용량을 예측한다.
     *
     * @param steps 예측할 시점의 수 (여기서는 1만 지원)
     * @return 예측된 다음 시점의 각 리소스 사용량 배열
     */
    private double[] forecast(int steps) {
        if (steps != 1) {
            throw new UnsupportedOperationException("현재는 steps=1 만 지원합니다.");
        }
        // 예제에서는 단순화를 위해, 계수 행렬 각 행의 평균을 마지막 관측값의 대용값으로 사용한다.
        int d = coefficients.getRowDimension();
        double[] lastObservation = new double[d];
        for (int i = 0; i < d; i++) {
            double[] row = coefficients.getRow(i);
            double sum = 0;
            for (double v : row) {
                sum += v;
            }
            lastObservation[i] = sum / row.length;
        }
        RealMatrix lastObsMatrix = MatrixUtils.createColumnRealMatrix(lastObservation);
        RealMatrix forecastMatrix = coefficients.multiply(lastObsMatrix);
        return forecastMatrix.getColumn(0);
    }
 
}

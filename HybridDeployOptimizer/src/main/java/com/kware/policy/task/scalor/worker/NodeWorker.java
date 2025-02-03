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
import java.util.stream.Collectors;

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
		if("N".equals(nsp.getScalingAt())){
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
				
				//{{10분 이상 지속되는지 확인
				//10분 이상인 노드의 갯수를 확인하고 처리해야하는데 빼먹었네.
				this.generateTimeBoundCluster(n.getClUid(), n.getNode(), snode, nsp, 10);
				
				//}}
			}else {
				//포함되지 않으면 모두 제거
				removeHighLow(n.getClUid(), n.getNode());
			}		
		}
		
		//요청처리함
		sendNodeScaling(clusterNodeCntMap, nsp);
		
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

		List<Integer> removeClUidList = new ArrayList<Integer>();
		Iterator<Integer> iter = cluster_high_nodes.keySet().iterator();
		while(iter.hasNext()) {
			clUid = iter.next();
			cluster_nodes = cluster_high_nodes.get(clUid);
			count = cluster_nodes.size(); //클러스터내의 지속되는 고부하 노드의 갯수
			cur_node_cnt = clusterNodeCntMap.get(clUid);  //클러스터의 현재 노드 갯수
			
			String[] node_names = cluster_nodes.keySet().toArray(new String[0]);
			
			if(count == cur_node_cnt && cur_node_cnt < nsp.getMaxCount()) { //현재 노드 갯수가  정책 최대값 보다 더 작으면 키우는 요청
				nodScalingInfos = calculateAverage(cluster_nodes);
				command = new WorkloadCommand<List<NodeScalingInfo>>(WorkloadCommand.CMD_NODE_SCALING_OUT, nodScalingInfos);
				WorkloadCommandManager.addCommand(command);
				
				this.comService.createEvent("노드 스케일 아웃[OUT] 판단", "NodeScale"
						, "클러스터의 고부하 노드로 인한 스케일 요청.\n클러스터 아이디:" + clUid 
						+ ", 고부하 노드:" + String.join(",", node_names)
				        + ", API 요청 여부 정책:" + nsp.getScalingAt());
				log.info("노드 스케일링 OUT 판단: cluster={},  고부하 nodes={}, API 요청 여부 정책: {}",  clUid, nodScalingInfos, nsp.getScalingAt());
				
			}
			
			removeClUidList.add(clUid);
			//removeHighLow(clUid); 
		}
		
		iter = cluster_low_nodes.keySet().iterator();
		while(iter.hasNext()) {
			clUid = iter.next();
			cluster_nodes = cluster_low_nodes.get(clUid);
			count = cluster_nodes.size(); //클러스터내의 지속되는 고부하 노드의 갯수
			cur_node_cnt = clusterNodeCntMap.get(clUid);  //클러스터의 현재 노드 갯수
			
			String[] node_names = cluster_nodes.keySet().toArray(new String[0]);
			
			if(cur_node_cnt > nsp.getMinCount()) { //현재 노드 갯수가 정책 최소값 보다 더 크면 줄이는 요청
				nodScalingInfos = calculateAverage(cluster_nodes);
				command = new WorkloadCommand<List<NodeScalingInfo>>(WorkloadCommand.CMD_NODE_SCALING_IN, nodScalingInfos);
				WorkloadCommandManager.addCommand(command);
				
				this.comService.createEvent("노드 스케일 인[IN] 판단", "NodeScale"
						, "클러스터의 여유 노드로 인한 스케일 요청.\n클러스터 아이디:" + clUid 
						+ ", 여유 노드:" + String.join(",", node_names)
						+ ", API 요청 여부 정책:" + nsp.getScalingAt());
				
				log.info("노드 스케일링 IN 판단: cluster={}, 여유 nodes={}, API 요청 여부 정책: {}",  clUid, nodScalingInfos, nsp.getScalingAt());
			}
			
			removeClUidList.add(clUid);
			//removeHighLow(clUid);
		}
		
		for(Integer i : removeClUidList) {
			removeHighLow(i);
		}
		removeClUidList.clear();
		
		//}}
		
	}
	
	
	// 평균을 계산하는 메서드
    public List<NodeScalingInfo> calculateAverage(Map<String, List<NodeScalingInfo>> clusterNodes) {
    	
    	return clusterNodes.values().stream()
                .map(nodeList -> {
                    double avgCpu  = nodeList.stream().mapToDouble(n -> n.cpu_per).average().orElse(0.0);
                    double avgMem  = nodeList.stream().mapToDouble(n -> n.mem_per).average().orElse(0.0);
                    double avgDisk = nodeList.stream().mapToDouble(n -> n.disk_per).average().orElse(0.0);
                    double avgGpu  = nodeList.stream().mapToDouble(n -> n.gpu_per).average().orElse(0.0);

                    NodeScalingInfo n = new NodeScalingInfo();
                    n.cpu_per  = avgCpu;
                    n.mem_per  = avgMem;
                    n.gpu_per  = avgGpu;
                    n.disk_per = avgDisk;
                    n.cur_node = nodeList.get(0).cur_node;
                    
                    return n;
                })
                .collect(Collectors.toList());    
    }
	
	
	/**
	 * 해당 노드가 10분간 지속되고 있는지 판단
	 * @param clUid
	 * @param nodeName
	 * @param snode
	 * @return
	 */
	private boolean generateTimeBoundCluster(Integer clUid, String nodeName, NodeScalingInfo snode, NodeScalingPolicy nsp, int minute) {
		Map<Integer, Map<String, List<NodeScalingInfo>>> cluster_nodes = null;
		if(snode.isHigh == true) {
			cluster_nodes = cluster_high_nodes;
		}else if(snode.isHigh == false) {
			cluster_nodes = cluster_low_nodes;
		}else {
			return false;
		}
		
		Map<String, List<NodeScalingInfo>> snodesMap = cluster_nodes.get(clUid);
		List<NodeScalingInfo> snodes = null;
		if(snodesMap != null) {		
			snodes = snodesMap.get(nodeName);
			if(snodes.size() > minute) { //snodes는 1분에 한번씩 수집되는 데이터이므로 10개를 10분으로 고려함 (10 분간 지속됨)
				return true;
 			}else {//해당되지 안으면 제거
 				snodes.clear();
 				snodesMap.remove(nodeName);
 			}
		}
		
		return false;
	}
	
	
	/**
	 * 해당 노드가 10분간 지속되고 있는지 판단
	 * @param clUid
	 * @param nodeName
	 * @param snode
	 * @return
	 */
	/*
	private boolean isOverCluster(Integer clUid, String nodeName, NodeScalingInfo snode, NodeScalingPolicy nsp, int nodeCnt) {
		Map<Integer, Map<String, List<NodeScalingInfo>>> cluster_nodes = null;
		if(snode.isHigh == true) {
			cluster_nodes = cluster_high_nodes;
		}else if(snode.isHigh == false) {
			cluster_nodes = cluster_low_nodes;
		}else {
			return false;
		}
		
		Map<String, List<NodeScalingInfo>> snodesMap = cluster_nodes.get(clUid);
		List<NodeScalingInfo> snodes = null;
		if(snodesMap != null) {		
			snodes = snodesMap.get(nodeName);
			if(snodes.size() > 10) { //10분간 지속됨,
				//{{클러스터 노드의 최소 및 최대 갯수 확인 프로그램 추가
				if(snode.isHigh && nodeCnt < nsp.getMaxCount()) {
					return true;
				}else if(!snode.isHigh && nodeCnt > nsp.getMinCount()) {
					return true;
				}
				//}}
 			}else {//해당되지 안으면 제거
 				snodes.clear();
 				snodesMap.remove(nodeName);
 			}
		}
		
		return false;
	}
	*/
	
	/*
	private List<NodeScalingInfo> getHighLow(Integer clUid, String nodeName,	Map<Integer, Map<String, List<NodeScalingInfo>>> cluster_nodes) {
		Map<String, List<NodeScalingInfo>> snodesMap = cluster_nodes.get(clUid);
		List<NodeScalingInfo> snodes = null;
		if(snodesMap != null) {		
			snodes = snodesMap.get(nodeName);
		}
		
		return snodes;
	}
	*/
	
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
		}
		cluster_nodes.put(clUid, nodesMap);
		
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
}

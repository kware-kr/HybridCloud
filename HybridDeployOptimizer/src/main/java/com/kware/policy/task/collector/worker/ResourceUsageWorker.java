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
package com.kware.policy.task.collector.worker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kware.common.util.JSONUtil;
import com.kware.policy.task.collector.service.ResourceUsageService;
import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.collector.service.vo.PromMetricNodes;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.collector.service.vo.PromMetricPods;
import com.kware.policy.task.collector.service.vo.ResourceUsageNode;
import com.kware.policy.task.collector.service.vo.ResourceUsagePod;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.PromQueue.PromDequeName;

import lombok.extern.slf4j.Slf4j;

/**
 * 리소스 사용량(노드, 파드)를 1분에 한번씩 DB에 저장
 */

@Slf4j
public class ResourceUsageWorker extends Thread {
	final QueueManager qm = QueueManager.getInstance();
		
	APIQueue apiQ   = qm.getApiQ();
	PromQueue promQ = qm.getPromQ();
	
	 // static ThreadFactory 정의
    private static final ThreadFactory namedThreadFactory = new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1); // 스레드 번호를 위한 카운터
        private final String namePrefix = "ResourceUsageThread-"; // 프리픽스 설정

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            return t;
        }
    };
    
	private static final ExecutorService executor = Executors.newFixedThreadPool(5, namedThreadFactory);
	
	ResourceUsageService service = null;
	boolean isRunning = false;

	public boolean isRunning() {
		return this.isRunning;
	}

	public void setResourceUsageServiceService(ResourceUsageService cmService) {
		this.service = cmService;
	}
	
	//json 변환시 필요없는 항목을 제거하기위함
	private final Set<String> labelFilterSet = new HashSet<String>(); 
	{
		Collections.addAll(labelFilterSet, "labels");
	}

	@Override
	public void run() {
		this.isRunning = true;
		try {
			//스레드 풀을 쓰는 방법도 고민해볼까?
			PromMetricNodes pmns = (PromMetricNodes)promQ.getPromDequesFirstObject(PromDequeName.METRIC_NODEINFO);
			Map<String, PromMetricNode>  pmnMap = pmns.getNodesMap();
			if(pmnMap != null) {
				for (PromMetricNode node : pmnMap.values()) {
		            Runnable task = new Runnable() {
		            	ResourceUsageNode usNode = null;
		                @Override
		                public void run() {
		                    // 데이터베이스에 저장
		                	usNode = new ResourceUsageNode(); 
		                	usNode.setClUid(node.getClUid());
		                	usNode.setCollectDt(node.getCollectDt());
		                	usNode.setNodeNm(node.getNode());
		                	
		                	String temp = null;
							try {
								temp = JSONUtil.getJsonstringFromObject(node, labelFilterSet);
							} catch (JsonProcessingException e) {}
		                	usNode.setResults(temp);
		                    service.insertResourceUsageNode(usNode);
		                }
		            };
		            // 작업 제출
		            executor.submit(task);
		        }
			}
			
			PromMetricPods pmps = (PromMetricPods)promQ.getPromDequesFirstObject(PromDequeName.METRIC_PODINFO);
			Map<String, PromMetricPod> pmpMap = pmps.getPodsMap();
			
			if(pmpMap != null) {
				for (PromMetricPod pod : pmpMap.values()) {
		            Runnable task = new Runnable() {
		            	ResourceUsagePod usPod = null;
		                @Override
		                public void run() {
		                    // 데이터베이스에 저장
		                	usPod = new ResourceUsagePod();
		                	usPod.setClUid(pod.getClUid());
		                	usPod.setMlId(pod.getMlId());
		                	usPod.setPodUid(pod.getPodUid());
		                	usPod.setCollectDt(pod.getCollectDt());
		                	
		                	String temp = null;
							try {
								temp = JSONUtil.getJsonstringFromObject(pod, labelFilterSet);
							} catch (JsonProcessingException e) {}
		                	usPod.setResults(temp);
		                	
		                    service.insertResourceUsagePod(usPod);
		                }
		            };
		            // 작업 제출
		            executor.submit(task);
		        }
			}
		}catch(Exception e) {
			log.error("ResourceUsageWorker Thread Error:" , e);
		}

	}
}

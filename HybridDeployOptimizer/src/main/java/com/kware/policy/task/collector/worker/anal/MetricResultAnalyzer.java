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
package com.kware.policy.task.collector.worker.anal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.kware.policy.task.collector.service.vo.ClusterWorkload;
import com.kware.policy.task.collector.service.vo.ClusterWorkloadPod;
import com.kware.policy.task.collector.service.vo.ClusterWorkloadResource;
import com.kware.policy.task.collector.service.vo.PromMetricContainer;
import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.collector.service.vo.PromMetricNodes;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.collector.service.vo.PromMetricPods;
import com.kware.policy.task.common.PromQLManager;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.constant.StringConstant;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

/**
 * collectMain > collectWorker에서 수집후 입력된 큐에서 데이터를 take하면서 파싱한다.
 */

@Slf4j
//@SuppressWarnings("rawtypes")
public class MetricResultAnalyzer {
	private static final Logger parseLog = LoggerFactory.getLogger("parse-log");

	private final static String JPATH_RESULT = "$.data.result";
	private final static String JPATH_DELIMIT_$ = "$";
	private final static String JPATH_DELIMIT_COMMA = ",";
	private final static String JPATH_DELIMIT_CONCAT = ":";
//	private final static String JPATH_METRIC = "$.metric";
//	private final static String JPATH_VALUE  = "$.value";
	private final static String JPATH_ALL = "$..*";

	private final static String JPATH_KEY_key = "key";
	private final static String JPATH_KEY_node = "node";
	private final static String JPATH_KEY_instance = "instance";
	private final static String JPATH_KEY_timestamp = "timestamp";
	private final static String JPATH_KEY_cl_uid = "cl_uid";

	private final static String JPATH_KEY_pod = "pod";
	private final static String JPATH_KEY_uid = "uid";
	private final static String JPATH_KEY_container = "container";
	private final static String JPATH_VAL_node_info = "node_info";
	private final static String JPATH_VAL_pod_info = "pod_info";
	private final static String JPATH_VAL_container_info = "container_info";

	// {{ json path에서 key값과 연동해서 key와 함수(Method)를 연결하기 위해 final static 으로 생성한
	// Mapping Map
	final Map<String, Method> m_nodeMethdMap = PromMetricNode.m_nodeMethodMap;
	final Map<String, Method> m_podMethdMap = PromMetricPod.m_podMethodMap;
	final Map<String, Method> m_containerMethdMap = PromMetricContainer.m_containerMethodMap;
	// }}

	QueueManager qm = QueueManager.getInstance();
	APIQueue apiQ = qm.getApiQ();
	PromQueue promQ = qm.getPromQ();

	final Long current_millitime;
	final Timestamp timestamp;
	final PromMetricNodes prom_nodes;
	final PromMetricPods prom_pods;
	
	public MetricResultAnalyzer(PromMetricNodes _nodes, PromMetricPods _pods, Long _current_millitime) {
		this.prom_nodes = _nodes;
		this.prom_pods = _pods;
		this.current_millitime = _current_millitime;
		this.timestamp = new Timestamp(current_millitime);
	}

	PromQLManager mp = PromQLManager.getInstance();
	HashMap<String, Object> clusterInfo = null;
	
	// int clUid;
	int prqlUid;

	public void analyze(Integer _clUid, int _prqlUid, String _prqlResult) {
		this.prqlUid = _prqlUid;
		int clUid;

		if (parseLog.isDebugEnabled()) {
			parseLog.debug(
					"-------------------------------------------------------------------------------------------");
			// log.debug("{}\n{}", jpath, result);
			parseLog.debug("result: {}", _prqlResult);
			parseLog.debug("prql_uid: {}", _prqlUid);
		}

		// {{API를 통해 조회된 workload
		Map<String, ClusterWorkload> apiWorkloadMap = this.apiQ.getApiWorkloadMap();
		Map<String, ClusterWorkloadPod> apiWorkloadPodMap = this.apiQ.getApiWorkloadPodMap();
		// }}API

		List<Map<String, Object>> resultList = JsonPath.read(_prqlResult, JPATH_RESULT);
		try {
			for (Map<String, Object> t : resultList) {
				/*
				 * String singleResult = JSONUtil.getJsonStringFromMap(t); t.clear();
				 * Map<String, Object> extractMap = this.extract(singleResult);
				 */
				Map<String, Object> extractMap = this.extract(t);
				t.clear();

				String sExtPath_key = (String) extractMap.remove(JPATH_KEY_key); // 공통
				String sExtPath_node = (String) extractMap.remove(JPATH_KEY_node); // 공통
				String sExtPath_instance = (String) extractMap.remove(JPATH_KEY_instance); // 공통
				Double sExtPath_timestamp = (Double) extractMap.remove(JPATH_KEY_timestamp); // 공통

				String sExtPath_cl_uid;
				if (_clUid == null) {
					sExtPath_cl_uid = (String) extractMap.remove(JPATH_KEY_cl_uid);// 공통
					try {
						clUid = Integer.parseInt(sExtPath_cl_uid);
					} catch (NumberFormatException nfe) {
						parseLog.error("clUid NumberFormatException:", nfe);
						clUid = -1;
					}
				} else
					clUid = _clUid;

				if (JPATH_VAL_node_info.equals(sExtPath_key)) { // 메트릭 key값
					// node 정보
					PromMetricNode node = this.prom_nodes.getMetricNode(clUid, sExtPath_node);
					if (node == null) {
						node = new PromMetricNode();
						node.setClUid(clUid);
						node.setNode(sExtPath_node);
						// node.setNoUid(sExtPath_node); //테스트로 일단 node값과 동일한 값
						node.setCollectDt(this.timestamp);
						node.setTimemillisecond(current_millitime);
						node.setPromTimestamp(new Timestamp(sExtPath_timestamp.longValue() * 1000));
						this.prom_nodes.setMetricNode(node);
					}
					node = (PromMetricNode) this.makePareData(node, extractMap);
				} else if (JPATH_VAL_pod_info.equals(sExtPath_key)) {
					String sExtPath_pod = (String) extractMap.remove(JPATH_KEY_pod); // 공통
					String sExtPath_puid = (String) extractMap.remove(JPATH_KEY_uid); // 공통

					// {{ portal api를 통한 uid가 있는 경우만 처리한다.
					String mlId = null;
					ClusterWorkloadPod wpod = apiWorkloadPodMap.get(sExtPath_puid);
					if (wpod == null) {
						extractMap.clear();
						extractMap = null;
						continue;
					} else {
						mlId = wpod.getMlId();

						// {{ workload api에는 클러스터 uid가 없어서 여기에서 처리한다.::20240905에 API에 추가됨
						ClusterWorkload clWorkload = apiWorkloadMap.get(mlId);
						/*
						if(clWorkload.getClUid() == null){
							clWorkload.setClUid(clUid);
						}
						*/

						for(Map.Entry<String, ClusterWorkloadResource> resourceE : clWorkload.getResourceMap().entrySet() ) {
							resourceE.getValue().setClUid(clUid);
							
							Map<String, ClusterWorkloadPod> podMap = resourceE.getValue().getPodMap();
							for (Map.Entry<String, ClusterWorkloadPod> entry : podMap.entrySet()) {
								entry.getValue().setClUid(clUid);
							}
						}
					}
					// }}

					// pod 정보
					PromMetricPod pod = this.prom_pods.getMetricPod(clUid, sExtPath_puid);
					if (pod == null) {
						pod = new PromMetricPod();
						pod.setClUid(clUid);
						pod.setNode(sExtPath_node);
						// pod.setNoUid(sExtPath_node); //테스트로 일단 node값과 동일한 값
						pod.setPod(sExtPath_pod);
						pod.setPodUid(sExtPath_puid);
						pod.setCollectDt(this.timestamp);
						pod.setTimemillisecond(current_millitime); // timestamp와 동일한 값인데
						pod.setMlId(mlId); // api에서 수집한 mlid 등록
						pod.setPromTimestamp(new Timestamp(sExtPath_timestamp.longValue() * 1000));
						this.prom_pods.setMetricPod(pod);
					}
					pod = (PromMetricPod) this.makePareData(pod, extractMap);
					
					ClusterWorkloadPod cwPod = apiQ.getApiWorkloadPodMap().get(pod.getPodUid());
					if(cwPod != null) {
						if(pod.isCompleted())
							cwPod.setCompleted(true);
					}

					// if(pod != null) //테스트
					// continue;
				} else if (JPATH_VAL_container_info.equals(sExtPath_key)) {
					String sExtPath_pod = (String) extractMap.remove(JPATH_KEY_pod); // 공통
					String sExtPath_puid = (String) extractMap.remove(JPATH_KEY_uid); // 공통
					String sExtPath_container = (String) extractMap.remove(JPATH_KEY_container); // 공통

					// {{portal api를 통한 uid가 있는 경우만 처리한다.
					if (!apiWorkloadPodMap.containsKey(sExtPath_puid)) {
						extractMap.clear();
						extractMap = null;
						continue;
					}
					// }}

					// node 정보
					PromMetricPod pod = this.prom_pods.getMetricPod(clUid, sExtPath_puid);
					if (pod == null) {
						pod = new PromMetricPod();
						pod.setClUid(clUid);
						pod.setNode(sExtPath_node);
						// pod.setNoUid(sExtPath_node); //테스트로 일단 node값과 동일한 값
						pod.setPod(sExtPath_pod);
						pod.setPodUid(sExtPath_puid);
						pod.setTimemillisecond(current_millitime);
						pod.setCollectDt(this.timestamp);
						pod.setPromTimestamp(new Timestamp(sExtPath_timestamp.longValue() * 1000));// 크게 의미 없는 정보
						this.prom_pods.setMetricPod(pod);
					}

					// container
					Map<String, PromMetricContainer> mContainerList = pod.getMContainerList();
					PromMetricContainer container = mContainerList.get(sExtPath_container);

					if (container == null) {
						container = new PromMetricContainer();
						container.setClUid(clUid);
						container.setNode(sExtPath_node);
						// pod.setNoUid(sExtPath_node); //테스트로 일단 node값과 동일한 값
						container.setPod(sExtPath_pod);
						container.setPodUid(sExtPath_puid);
						container.setContainer(sExtPath_container);
						container.setPromTimestamp(new Timestamp(sExtPath_timestamp.longValue() * 1000));

						mContainerList.put(sExtPath_container, container);
					}
					container = (PromMetricContainer) this.makePareData(container, extractMap);

					// if(container != null) //테스트
					// continue;
				}

				if (extractMap != null) {
					extractMap.clear();
					extractMap = null;
				}
			} // }}end for
		} catch (Exception e) {
			parseLog.error("error", e);
		}

		if (resultList != null)
			resultList.clear();
		resultList = null;
	}

	/**
	 * json데이터를 추출하기 위한 jsonpath가 다음처럼 정의되어 있음 { "key": "node_info", "node":
	 * "$.metric.Hostname", "instance":
	 * "$.metric.instance,$.metric.kubernetes_io_hostname", "timestamp":
	 * "$.value[0]", "gpu_temp_$.metric.gpu": "$.value[1]" }
	 * 
	 * @param _prqlUid
	 * @param _prqlSingleResult
	 * @return
	 */
	// private Map<String, Object> extract(String _prqlSingleResult) {
	private Map<String, Object> extract(Object _prqlSingleResult) {

		if (prqlUid == 49) {
			parseLog.info("=> prqlUid:{}", prqlUid);
		}
		if (parseLog.isDebugEnabled()) {
			parseLog.debug("temp_result: {}", _prqlSingleResult);
		}

		Map<String, Object> mRs = new HashMap<String, Object>();
		Object val_obj = null;

		try {
			DocumentContext ctx = JsonPath.parse(_prqlSingleResult);

			// {{for key
			Map<String, Object> jpathMap = mp.getExtractPathMap(prqlUid); // 관련데이터 내부를 삭제하면 안된다. readOnly Map.
			for (String key : jpathMap.keySet()) {
				String path = (String) jpathMap.get(key);

				// {"requests_$.metric.resource": "$.metric.host_ip, $.metric.instance"} 중에서
				// key와 관련 부분
				int key_index = key.indexOf(JPATH_DELIMIT_$);

				String temp_key = key;
				if (key_index != -1) {
					String key_path = key.substring(key_index);
					temp_key = key.substring(0, key_index) + ctx.read(key_path);
				}
				key = temp_key;
				// }}key값 처리 완료

				// {{ value에 있는 path처리: path에는 ",",":", "함수" 가 있음
				// 배열로 변경: {"requests_$.metric.resource": "$.metric.host_ip, $.metric.instance"}
				// 중에서 value와 관련 부분
				String[] path_array = path.split(JPATH_DELIMIT_COMMA);
				for (String temp_path : path_array) {
					// {{ 연결 연산자 처리
					String[] concat_array = temp_path.split(JPATH_DELIMIT_CONCAT);
					if (concat_array.length > 1) { // 구분자가 있는 경우만 처리하고
						StringBuilder sb = new StringBuilder();
						for (String c : concat_array) {

							Object o = this.getJpathRead(ctx, c.trim());
							if (o != null) {
								if (sb.length() > 0) {
									sb.append(JPATH_DELIMIT_CONCAT);
								}
								sb.append(o.toString());
							}
						}
						val_obj = sb.toString();
						break; //
					} else {
						val_obj = this.getJpathRead(ctx, temp_path.trim());
						// parseLog.info("k,v {},{}",key, val_obj == null? "null": val_obj.toString());
					}
				}
				mRs.put(temp_key, val_obj);
			}
			// }}end for

			/*
			 * 테스트1 jsonpath에서 조회한 결과는 모두 ObjectMapper를 통해 변환하던지, 아니면 개별적인 객체로 생성하는 방식으로
			 * 처리해야겠네. Map<String,Object> tempMap = ctx.read("$.metric");
			 * 
			 * ObjectMapper objectMapper = new ObjectMapper();
			 * 
			 * JsonNode resultNode = objectMapper.valueToTree(tempMap);
			 * 
			 * // resultNode를 깊은 복사 JsonNode deepClonedResultNode =
			 * objectMapper.readTree(resultNode.toString());
			 * 
			 * // 깊은 복사된 JSON 출력 String clonedJsonString =
			 * objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
			 * deepClonedResultNode); System.out.println("Cloned JSON String:");
			 * System.out.println(clonedJsonString);
			 */

			/* 테스트2 */
			/*
			 * Map<String,Object> tempMap = ctx.read("$.metric"); Map<String,Object> tMap =
			 * (Map)SerializationUtils.clone((HashMap)tempMap);
			 */

			// 파싱한 객제 모두 제거
			// 이전에 조회하거나 가지고 온 객체의 데이터도 제거된다, 즉 값을 처리하고나, deepClone을 하지 않으면 삭제된다.
			ctx.delete(JPATH_ALL);
			ctx = null;
		} catch (Exception e) {
			parseLog.error("error", e);
		}

		if (parseLog.isDebugEnabled())
			parseLog.debug("END Extract prqlUid:{}", prqlUid);

		return mRs;
	}

	/**
	 * DocumentContext에 JSONPath를 통해 해당하는 value를 조회하는 함수
	 * 
	 * @param _ctx  ; json문서가 parsing된 DocumentContext instance
	 * @param path: jsonpath
	 * @return
	 */
	private Object getJpathRead(DocumentContext _ctx, String path) {
		Object val_obj = null;
		int path_index = path.indexOf(JPATH_DELIMIT_$);

		if (path_index != -1) {
			try {
				val_obj = _ctx.read(path);
				if (val_obj instanceof JSONArray) {
					if (((JSONArray) val_obj).isEmpty()) {
						val_obj = null;
					} else {
						val_obj = ((JSONArray) val_obj).get(0);
					}
				}
			} catch (PathNotFoundException e) {
				log.error("PathNotFoundException:[{}]", this.prqlUid, e);
				val_obj = null;
			} catch (InvalidPathException e) {
				val_obj = InvalidPathExceptionProcess(_ctx, path, e.getMessage());
			}

		} else {
			val_obj = path; // 값중에 상수값이 있을 수 있음
		}
		return val_obj;
	}

	/**
	 * PromMetricNode, PromMetricPod, PromMetricContainer에 각 데이터를 매핑하는 함수 이미 등록된 각
	 * Object Method를 통해서 멤버 함수 호출을 통해 데이터를 설정한다.
	 * 
	 * @param _metricObj
	 * @param _valsMap
	 * @return
	 */
	private Object makePareData(Object _metricObj, Map<String, Object> _valsMap) {
		if (parseLog.isInfoEnabled())
			parseLog.info("makePareData: {}", Objects.toString(_valsMap, null));

		_valsMap.forEach((key, value) -> {
			if (parseLog.isInfoEnabled())
				parseLog.info("Mapping Target: {}  k,v:{},{}", value == null ? null : value.getClass().getName(), key,
						Objects.toString(value, null));

			Method m = null;

			if (_metricObj instanceof PromMetricNode)
				m = m_nodeMethdMap.get(key);
			else if (_metricObj instanceof PromMetricPod)
				m = m_podMethdMap.get(key);
			else if (_metricObj instanceof PromMetricContainer)
				m = m_containerMethdMap.get(key);

			if (m != null) {
				Class<?> type = m.getParameterTypes()[0];
				try {
					if (value == null) {
						// m.invoke(_metricObj, value);
						return;
					}

					if (type == String.class) {
						m.invoke(_metricObj, value);
					} else if (type == int.class || type == Integer.class) {
						Double d = Double.parseDouble(value.toString());
						m.invoke(_metricObj, d.intValue());
					} else if (type == boolean.class || type == Boolean.class) {
						String t = value.toString();
						if (StringConstant.STR_0.equals(t)) {
							m.invoke(_metricObj, Boolean.FALSE);
						} else if (StringConstant.STR_1.equals(t)) {
							m.invoke(_metricObj, Boolean.TRUE);
						} else {
							m.invoke(_metricObj, Boolean.parseBoolean(value.toString()));
						}
					} else if (type == long.class || type == Long.class) {
						Double d = Double.parseDouble(value.toString());
						m.invoke(_metricObj, d.longValue());
					} else if (type == double.class || type == Double.class) {
						m.invoke(_metricObj, Double.parseDouble(value.toString()));
					} else if (type == float.class || type == Float.class) {
						m.invoke(_metricObj, Float.parseFloat(value.toString()));
					} else if (type == short.class || type == Short.class) {
						m.invoke(_metricObj, Short.parseShort(value.toString()));
					} else if (type == byte.class || type == Byte.class) {
						m.invoke(_metricObj, Byte.parseByte(value.toString()));
					} else if (type == Timestamp.class) {
						long t = Long.parseLong(value.toString());
						m.invoke(_metricObj, new Timestamp(t * 1000));
					} else {
						m.invoke(_metricObj, value);
					}

				} catch (IllegalAccessException e) {
					parseLog.error("", e);
				} catch (IllegalArgumentException e) {
					parseLog.error("", e);
				} catch (InvocationTargetException e) {
					parseLog.error("", e);
				}
			}
		});
		return _metricObj;
	}

	/**
	 * jsonpath에 없는 함수 등이 있는 경우 에러 처리
	 * 
	 * @param ctx
	 * @param org_path
	 * @param message
	 * @return
	 */
	private Object InvalidPathExceptionProcess(DocumentContext ctx, String org_path, String message) {
		int index, rp_index, lp_index;
		String function, path, fn_arg;
		if (message.startsWith("Function")) {
			index = org_path.lastIndexOf(".");
			function = org_path.substring(index + 1);
			path = org_path.substring(0, index);
			rp_index = function.indexOf('(');
			lp_index = function.indexOf(')');

			fn_arg = function.substring(rp_index + 1, lp_index);

			if (function.startsWith("startWith(")) {
				return fn_startWith(ctx, path, fn_arg);
			}
		}

		return null;
	}

	/**
	 * jsonpath에 커스텀함수 startWith를 만들어서 이를 처리하는 루틴 생성
	 * 
	 * @param ctx
	 * @param path
	 * @param arg
	 * @return
	 */
	private Object fn_startWith(DocumentContext ctx, String path, String arg) {
		Map<String, Object> jsonObject = ctx.read(path);

		Map<String, Object> resultObject = new HashMap<String, Object>();
		// 기존방법
		Iterator<String> iterator = jsonObject.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			if (key.startsWith(arg)) {
				resultObject.put(key, jsonObject.get(key));
				iterator.remove();
			}
		}
		return resultObject;
	}

	/**
	 * main test
	 * 
	 * @param args
	 */
	// public static void main(String[] args) {
	/*
	 * 
	 * String a =
	 * "{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[{\"metric\":{\"__name__\":\"kube_node_labels\",\"container\":\"kube-state-metrics\",\"endpoint\":\"http\",\"instance\":\"192.168.82.29:8080\",\"job\":\"kube-prometheus-kube-state-metrics\",\"label_beta_kubernetes_io_arch\":\"amd64\",\"label_beta_kubernetes_io_os\":\"linux\",\"label_feature_node_kubernetes_io_cpu_cpuid_cmpxchg8\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_fxsr\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_fxsropt\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_hypervisor\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_lahf\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_osxsave\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_syscall\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_sysee\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_x87\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_xsave\":\"true\",\"label_feature_node_kubernetes_io_cpu_hardware_multithreading\":\"false\",\"label_feature_node_kubernetes_io_cpu_model_family\":\"15\",\"label_feature_node_kubernetes_io_cpu_model_id\":\"6\",\"label_feature_node_kubernetes_io_cpu_model_vendor_id\":\"AMD\",\"label_feature_node_kubernetes_io_kernel_config_no_hz\":\"true\",\"label_feature_node_kubernetes_io_kernel_config_no_hz_idle\":\"true\",\"label_feature_node_kubernetes_io_kernel_version_full\":\"5.15.0-94-generic\",\"label_feature_node_kubernetes_io_kernel_version_major\":\"5\",\"label_feature_node_kubernetes_io_kernel_version_minor\":\"15\",\"label_feature_node_kubernetes_io_kernel_version_revision\":\"0\",\"label_feature_node_kubernetes_io_pci_10de_present\":\"true\",\"label_feature_node_kubernetes_io_pci_1234_present\":\"true\",\"label_feature_node_kubernetes_io_pci_15ad_present\":\"true\",\"label_feature_node_kubernetes_io_system_os_release_id\":\"ubuntu\",\"label_feature_node_kubernetes_io_system_os_release_version_id\":\"22.04\",\"label_feature_node_kubernetes_io_system_os_release_version_id_major\":\"22\",\"label_feature_node_kubernetes_io_system_os_release_version_id_minor\":\"04\",\"label_kubernetes_io_arch\":\"amd64\",\"label_kubernetes_io_hostname\":\"kube-worker-a1\",\"label_kubernetes_io_os\":\"linux\",\"label_node_role_kubernetes_io_node\":\"true\",\"label_nvidia_com_cuda_driver_major\":\"535\",\"label_nvidia_com_cuda_driver_minor\":\"104\",\"label_nvidia_com_cuda_driver_rev\":\"05\",\"label_nvidia_com_cuda_runtime_major\":\"12\",\"label_nvidia_com_cuda_runtime_minor\":\"2\",\"label_nvidia_com_gfd_timestamp\":\"1708409129\",\"label_nvidia_com_gpu_compute_major\":\"8\",\"label_nvidia_com_gpu_compute_minor\":\"6\",\"label_nvidia_com_gpu_count\":\"1\",\"label_nvidia_com_gpu_deploy_container_toolkit\":\"true\",\"label_nvidia_com_gpu_deploy_dcgm\":\"true\",\"label_nvidia_com_gpu_deploy_dcgm_exporter\":\"true\",\"label_nvidia_com_gpu_deploy_device_plugin\":\"true\",\"label_nvidia_com_gpu_deploy_driver\":\"true\",\"label_nvidia_com_gpu_deploy_gpu_feature_discovery\":\"true\",\"label_nvidia_com_gpu_deploy_node_status_exporter\":\"true\",\"label_nvidia_com_gpu_deploy_operator_validator\":\"true\",\"label_nvidia_com_gpu_family\":\"ampere\",\"label_nvidia_com_gpu_memory\":\"24576\",\"label_nvidia_com_gpu_present\":\"true\",\"label_nvidia_com_gpu_product\":\"NVIDIA-GeForce-RTX-3090\",\"label_nvidia_com_gpu_replicas\":\"1\",\"label_nvidia_com_mig_capable\":\"false\",\"label_nvidia_com_mig_strategy\":\"single\",\"namespace\":\"monitoring\",\"node\":\"kube-worker-a1\",\"pod\":\"kube-prometheus-kube-state-metrics-755d857b47-2vxdj\",\"service\":\"kube-prometheus-kube-state-metrics\"},\"value\":[1716451473.466,\"1\"]},{\"metric\":{\"__name__\":\"kube_node_labels\",\"container\":\"kube-state-metrics\",\"endpoint\":\"http\",\"instance\":\"192.168.82.29:8080\",\"job\":\"kube-prometheus-kube-state-metrics\",\"label_beta_kubernetes_io_arch\":\"amd64\",\"label_beta_kubernetes_io_os\":\"linux\",\"label_feature_node_kubernetes_io_cpu_cpuid_cmpxchg8\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_fxsr\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_fxsropt\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_hypervisor\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_lahf\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_osxsave\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_syscall\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_sysee\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_x87\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_xsave\":\"true\",\"label_feature_node_kubernetes_io_cpu_hardware_multithreading\":\"false\",\"label_feature_node_kubernetes_io_cpu_model_family\":\"15\",\"label_feature_node_kubernetes_io_cpu_model_id\":\"6\",\"label_feature_node_kubernetes_io_cpu_model_vendor_id\":\"AMD\",\"label_feature_node_kubernetes_io_kernel_config_no_hz\":\"true\",\"label_feature_node_kubernetes_io_kernel_config_no_hz_idle\":\"true\",\"label_feature_node_kubernetes_io_kernel_version_full\":\"5.15.0-94-generic\",\"label_feature_node_kubernetes_io_kernel_version_major\":\"5\",\"label_feature_node_kubernetes_io_kernel_version_minor\":\"15\",\"label_feature_node_kubernetes_io_kernel_version_revision\":\"0\",\"label_feature_node_kubernetes_io_pci_10de_present\":\"true\",\"label_feature_node_kubernetes_io_pci_1234_present\":\"true\",\"label_feature_node_kubernetes_io_pci_15ad_present\":\"true\",\"label_feature_node_kubernetes_io_system_os_release_id\":\"ubuntu\",\"label_feature_node_kubernetes_io_system_os_release_version_id\":\"22.04\",\"label_feature_node_kubernetes_io_system_os_release_version_id_major\":\"22\",\"label_feature_node_kubernetes_io_system_os_release_version_id_minor\":\"04\",\"label_kubernetes_io_arch\":\"amd64\",\"label_kubernetes_io_hostname\":\"kube-worker-a2\",\"label_kubernetes_io_os\":\"linux\",\"label_node_role_kubernetes_io_node\":\"true\",\"label_nvidia_com_cuda_driver_major\":\"535\",\"label_nvidia_com_cuda_driver_minor\":\"104\",\"label_nvidia_com_cuda_driver_rev\":\"05\",\"label_nvidia_com_cuda_runtime_major\":\"12\",\"label_nvidia_com_cuda_runtime_minor\":\"2\",\"label_nvidia_com_gfd_timestamp\":\"1708409129\",\"label_nvidia_com_gpu_compute_major\":\"8\",\"label_nvidia_com_gpu_compute_minor\":\"6\",\"label_nvidia_com_gpu_count\":\"1\",\"label_nvidia_com_gpu_deploy_container_toolkit\":\"true\",\"label_nvidia_com_gpu_deploy_dcgm\":\"true\",\"label_nvidia_com_gpu_deploy_dcgm_exporter\":\"true\",\"label_nvidia_com_gpu_deploy_device_plugin\":\"true\",\"label_nvidia_com_gpu_deploy_driver\":\"true\",\"label_nvidia_com_gpu_deploy_gpu_feature_discovery\":\"true\",\"label_nvidia_com_gpu_deploy_node_status_exporter\":\"true\",\"label_nvidia_com_gpu_deploy_operator_validator\":\"true\",\"label_nvidia_com_gpu_family\":\"ampere\",\"label_nvidia_com_gpu_memory\":\"24576\",\"label_nvidia_com_gpu_present\":\"true\",\"label_nvidia_com_gpu_product\":\"NVIDIA-GeForce-RTX-3090\",\"label_nvidia_com_gpu_replicas\":\"1\",\"label_nvidia_com_mig_capable\":\"false\",\"label_nvidia_com_mig_strategy\":\"single\",\"namespace\":\"monitoring\",\"node\":\"kube-worker-a2\",\"pod\":\"kube-prometheus-kube-state-metrics-755d857b47-2vxdj\",\"service\":\"kube-prometheus-kube-state-metrics\"},\"value\":[1716451473.466,\"1\"]},{\"metric\":{\"__name__\":\"kube_node_labels\",\"container\":\"kube-state-metrics\",\"endpoint\":\"http\",\"instance\":\"192.168.82.29:8080\",\"job\":\"kube-prometheus-kube-state-metrics\",\"label_beta_kubernetes_io_arch\":\"amd64\",\"label_beta_kubernetes_io_os\":\"linux\",\"label_feature_node_kubernetes_io_cpu_cpuid_cmpxchg8\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_fxsr\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_fxsropt\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_hypervisor\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_lahf\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_osxsave\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_syscall\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_sysee\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_x87\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_xsave\":\"true\",\"label_feature_node_kubernetes_io_cpu_hardware_multithreading\":\"false\",\"label_feature_node_kubernetes_io_cpu_model_family\":\"15\",\"label_feature_node_kubernetes_io_cpu_model_id\":\"6\",\"label_feature_node_kubernetes_io_cpu_model_vendor_id\":\"AMD\",\"label_feature_node_kubernetes_io_kernel_config_no_hz\":\"true\",\"label_feature_node_kubernetes_io_kernel_config_no_hz_idle\":\"true\",\"label_feature_node_kubernetes_io_kernel_version_full\":\"5.15.0-94-generic\",\"label_feature_node_kubernetes_io_kernel_version_major\":\"5\",\"label_feature_node_kubernetes_io_kernel_version_minor\":\"15\",\"label_feature_node_kubernetes_io_kernel_version_revision\":\"0\",\"label_feature_node_kubernetes_io_pci_1234_present\":\"true\",\"label_feature_node_kubernetes_io_pci_15ad_present\":\"true\",\"label_feature_node_kubernetes_io_system_os_release_id\":\"ubuntu\",\"label_feature_node_kubernetes_io_system_os_release_version_id\":\"22.04\",\"label_feature_node_kubernetes_io_system_os_release_version_id_major\":\"22\",\"label_feature_node_kubernetes_io_system_os_release_version_id_minor\":\"04\",\"label_kubernetes_io_arch\":\"amd64\",\"label_kubernetes_io_hostname\":\"kube-master-a0\",\"label_kubernetes_io_os\":\"linux\",\"label_node_role_kubernetes_io_master\":\"true\",\"namespace\":\"monitoring\",\"node\":\"kube-master-a0\",\"pod\":\"kube-prometheus-kube-state-metrics-755d857b47-2vxdj\",\"service\":\"kube-prometheus-kube-state-metrics\"},\"value\":[1716451473.466,\"1\"]},{\"metric\":{\"__name__\":\"kube_node_labels\",\"container\":\"kube-state-metrics\",\"endpoint\":\"http\",\"instance\":\"192.168.82.29:8080\",\"job\":\"kube-prometheus-kube-state-metrics\",\"label_beta_kubernetes_io_arch\":\"amd64\",\"label_beta_kubernetes_io_os\":\"linux\",\"label_feature_node_kubernetes_io_cpu_cpuid_cmpxchg8\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_fxsr\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_fxsropt\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_hypervisor\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_lahf\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_osxsave\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_syscall\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_sysee\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_x87\":\"true\",\"label_feature_node_kubernetes_io_cpu_cpuid_xsave\":\"true\",\"label_feature_node_kubernetes_io_cpu_hardware_multithreading\":\"false\",\"label_feature_node_kubernetes_io_cpu_model_family\":\"15\",\"label_feature_node_kubernetes_io_cpu_model_id\":\"6\",\"label_feature_node_kubernetes_io_cpu_model_vendor_id\":\"AMD\",\"label_feature_node_kubernetes_io_kernel_config_no_hz\":\"true\",\"label_feature_node_kubernetes_io_kernel_config_no_hz_idle\":\"true\",\"label_feature_node_kubernetes_io_kernel_version_full\":\"5.15.0-94-generic\",\"label_feature_node_kubernetes_io_kernel_version_major\":\"5\",\"label_feature_node_kubernetes_io_kernel_version_minor\":\"15\",\"label_feature_node_kubernetes_io_kernel_version_revision\":\"0\",\"label_feature_node_kubernetes_io_pci_1234_present\":\"true\",\"label_feature_node_kubernetes_io_pci_15ad_present\":\"true\",\"label_feature_node_kubernetes_io_system_os_release_id\":\"ubuntu\",\"label_feature_node_kubernetes_io_system_os_release_version_id\":\"22.04\",\"label_feature_node_kubernetes_io_system_os_release_version_id_major\":\"22\",\"label_feature_node_kubernetes_io_system_os_release_version_id_minor\":\"04\",\"label_kubernetes_io_arch\":\"amd64\",\"label_kubernetes_io_hostname\":\"kube-worker-a3\",\"label_kubernetes_io_os\":\"linux\",\"label_node_role_kubernetes_io_node\":\"true\",\"namespace\":\"monitoring\",\"node\":\"kube-worker-a3\",\"pod\":\"kube-prometheus-kube-state-metrics-755d857b47-2vxdj\",\"service\":\"kube-prometheus-kube-state-metrics\"},\"value\":[1716451473.466,\"1\"]}]}}";
	 * DocumentContext ctx = JsonPath.parse(a); String jsonpath =
	 * "{\"key\": \"node_info\", \"node\": \"$.metric.node\", \"label\": \"$.metric.startWith(label)\", \"instance\": \"$.metric.instance\", \"timestamp\": \"$.value[0]\"}"
	 * ;
	 * 
	 * MetricResultAnalyzer p = new MetricResultAnalyzer(); p.extract(1,1, a,
	 * jsonpath);
	 * 
	 */
	// }

}

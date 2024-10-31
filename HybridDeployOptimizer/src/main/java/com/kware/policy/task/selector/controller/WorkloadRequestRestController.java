package com.kware.policy.task.selector.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.kware.common.openapi.vo.APIResponse;
import com.kware.common.openapi.vo.APIResponseCode;
import com.kware.common.util.JSONUtil;
import com.kware.common.util.StringUtil;
import com.kware.common.util.YAMLUtil;
import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.constant.StringConstant.RequestStatus;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;
import com.kware.policy.task.common.service.CommonService;
import com.kware.policy.task.common.service.vo.CommonConfigGroup;
import com.kware.policy.task.selector.service.WorkloadRequestService;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 테스트를 위해서 진행함 1차 바로진행 2창 여기에서 rabbit mq로 보내고, rabbit mq에서 받아서 처리하고, mq에서 다시
 * 보내고, 받는 형태로 처리
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Slf4j
@RestController
@RequestMapping("/interface/api/v1")
@RequiredArgsConstructor
public class WorkloadRequestRestController {
	private final WorkloadRequestService wlService;
	private final CommonService cmService;

	QueueManager qm = QueueManager.getInstance();
	// APIQueue apiQ = qm.getApiQ();
	PromQueue promQ = qm.getPromQ();
	RequestQueue requestQ = qm.getRequestQ();

	// json parser 에러
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> handleValidationException(HttpMessageNotReadableException ex) {
		APIResponseCode errCode = APIResponseCode.INVALID_PARAMETER_ERROR;
		APIResponse<String> res = new APIResponse(errCode.getCode(), errCode.getMessage(), null);

		return ResponseEntity.ok(res);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleException(HttpServletRequest request, Exception ex) {
		log.error("Error 요청 URI:{}", request.getRequestURI(), ex);
		APIResponseCode errCode = APIResponseCode.INTERNAL_SYSTEM_ERROR;

		APIResponse<String> res = new APIResponse(errCode.getCode(), errCode.getMessage(), null);

		return ResponseEntity.ok(res);
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<?> handleDataAccessException(HttpServletRequest request, Exception ex) {
		log.error("Error 요청 URI:{}", request.getRequestURI(), ex);
		APIResponseCode errCode = APIResponseCode.INTERNAL_DATABASE_ERROR;

		APIResponse<String> res = new APIResponse(errCode.getCode(), errCode.getMessage(), null);

		return ResponseEntity.ok(res);
	}

	private String extractErrorMessage(Exception e) {
		if (e instanceof JsonMappingException) {
			JsonMappingException mappingException = (JsonMappingException) e;
			JsonLocation jl = mappingException.getLocation();
			return "[" + jl.offsetDescription() + "]";
		} else {
			return null;
		}
	}

	/**
	 * 배포가능한 최적의 노드를 조회(BestFitBinPacking)
	 * 
	 * @param yamlstring
	 * @return
	 * @throws Exception
	 */
	@PostMapping("/do/schedule/nodeselect")
	public ResponseEntity<?> getNodeBFBP(@RequestBody String requestString) throws Exception {
		APIResponseCode status = null;
		// WorkloadRequest wlRequest = YAMLUtil.read(requestString,
		// WorkloadRequest.class);
		if (log.isDebugEnabled())
			log.debug("배포요청 전문: {}", requestString);

		WorkloadRequest wlRequest = null;
		String errorMessage = null;
		try {
			wlRequest = JSONUtil.fromJsonToEmptyFromNull(requestString, WorkloadRequest.class);
		} catch (Exception e) {
			errorMessage = extractErrorMessage(e);
			if (errorMessage != null)
				log.error("Parser Error: {}", errorMessage, e);
			else
				throw e;
		}
		if (wlRequest == null) {
			status = APIResponseCode.INPUT_ERROR;
			APIResponse<String> ares = new APIResponse(status.getCode(), status.getMessage() + errorMessage, null);
			return ResponseEntity.ok(ares);
		}
		wlRequest.aggregate(true);

		// {{ WorkloadRequest DB저장
		WorkloadRequest.Request req = wlRequest.getRequest();
		// 단순한 DB저장을 위해 원본 그대로를 json으로 변환하기 위함
		// req.setInfo(YAMLUtil.convertYamlToJson(requestString));
		req.setInfo(requestString);
		req.setStatus(RequestStatus.request.toString());
		wlService.insertMoUserRequest(req);
		// }}

		// {{노드 셀렉터
		WorkloadResponse wlResponse = wlService.getResponseToSelectedNode(wlRequest);
		// }}

		// DB 저장
		wlService.insertMoUserResponse(wlResponse.getResponse());

		// WorkloadRequest에 Response 입력
		wlRequest.setResponse(wlResponse.getResponse());
		String res_yamlstring = YAMLUtil.writeString(wlRequest, false);
		wlResponse.getResponse().setOriginRequest(StringUtil.encodebase64(requestString));

		/*
		 * WorkloadResponse.Response wlResponseRes =wlResponse.getResponse();
		 * wlResponseRes.getResult().setOriginRequest(yamlstring); String res_yamlstring
		 * = YAMLUtil.writeString(wlResponseRes, false);
		 */

		APIResponse<String> ares = new APIResponse(wlResponse.getResponse().getCode(),
				wlResponse.getResponse().getMessage(), wlResponse.getResponse()
		);

		// return ResponseEntity.ok(wlRequest);
		return ResponseEntity.ok(ares);
	}

	/**
	 * 해당 워크로드의 노드 배포 완료
	 * 
	 * @param yamlstring
	 * @return
	 * @throws Exception
	 */
	@PostMapping("/do/schedule/nodeselect/complete")
	public ResponseEntity<?> getNodeScheduleComplete(@RequestBody String requestString) throws Exception {
		if (log.isDebugEnabled())
			log.debug("배포완료 통지 전문:{}", requestString);

		APIResponseCode status = null;
		WorkloadRequest wlRequest = null;
		String errorMessage = null;
		try {
			wlRequest = JSONUtil.fromJsonToEmptyFromNull(requestString, WorkloadRequest.class);
		} catch (Exception e) {
			errorMessage = extractErrorMessage(e);
			if (errorMessage != null)
				log.error("Parser Error: {}", errorMessage, e);
			else
				throw e;
		}
		if (wlRequest == null) {
			status = APIResponseCode.INPUT_ERROR;
			APIResponse<String> ares = new APIResponse(status.getCode(), status.getMessage() + errorMessage, null);
			return ResponseEntity.ok(ares);
		}

		if (log.isInfoEnabled())
			log.info("배포완료 통지 ID:{}", wlRequest.getRequest().getMlId());

		// {{ WorkloadRequest DB저장
		WorkloadRequest.Request req = wlRequest.getRequest();
		// 단순한 DB저장을 위해 원본 그대로를 json으로 변환하기 위함
		req.setInfo(requestString);
		req.setStatus(RequestStatus.complete.toString());
		wlService.insertMoUserRequest(req);
		// }}

		status = APIResponseCode.SUCCESS;
		APIResponse<String> ares = new APIResponse(status.getCode(), status.getMessage(), null);

		return ResponseEntity.ok(ares);
	}

	/**
	 * 배포가능한 노드의 전체 스코어 BestFitBinPacking 알고리즘 적용한 계산
	 * 
	 * @param yamlstring
	 * @return
	 */
	@PostMapping("/do/schedule/node_score_bfbp")
	public ResponseEntity<?> getNodeScoreBFBP(@RequestBody String requestString) throws Exception {
		// try {
		APIResponseCode status = null;
		WorkloadRequest wlRequest = null;
		String errorMessage = null;
		try {
			wlRequest = JSONUtil.fromJsonToEmptyFromNull(requestString, WorkloadRequest.class);
		} catch (Exception e) {
			errorMessage = extractErrorMessage(e);
			if (errorMessage != null)
				log.error("Parser Error: {}", errorMessage, e);
			else
				throw e;
		}
		if (wlRequest == null) {
			status = APIResponseCode.INPUT_ERROR;
			APIResponse<String> ares = new APIResponse(status.getCode(), status.getMessage() + errorMessage, null);
			return ResponseEntity.ok(ares);
		}

		wlRequest.aggregate(true);

		// {{ WorkloadRequest DB저장
		WorkloadRequest.Request req = wlRequest.getRequest();
		// 단순한 DB저장을 위해 원본 그대로를 json으로 변환하기 위함
		req.setInfo(requestString);
		req.setStatus(RequestStatus.request.toString());
		wlService.insertMoUserRequest(req);
		// }}

		// {{노드 셀렉터
		List<PromMetricNode> sel_nodes = wlService.getNodeScore(wlRequest, 0); // 전체 노드의 순서
		// }}

		return ResponseEntity.ok(sel_nodes);
	}

	/**
	 * 단순한 용량으로 스코어 계산
	 * 
	 * @param yamlstring
	 * @return
	 * @throws Exception
	 */
	// @PostMapping("/do/schedule/node_score_for_capacity")
	@RequestMapping(value = "/do/schedule/node_score_for_capacity", method = { RequestMethod.POST, RequestMethod.GET })
	public ResponseEntity<?> getNodeScroeCapacity(@RequestBody(required = false) String requestString)
			throws Exception {
		List<PromMetricNode> nodes = null;
		WorkloadRequest wlRequest = null;
		if (requestString == null) {
			nodes = promQ.getLastPromMetricNodesReadOnly();
		} else {
			APIResponseCode status = null;
			String errorMessage = null;
			try {
				wlRequest = JSONUtil.fromJsonToEmptyFromNull(requestString, WorkloadRequest.class);
			} catch (Exception e) {
				errorMessage = extractErrorMessage(e);
				if (errorMessage != null)
					log.error("Parser Error: {}", errorMessage, e);
				else
					throw e;
			}
			if (wlRequest == null) {
				status = APIResponseCode.INPUT_ERROR;
				APIResponse<String> ares = new APIResponse(status.getCode(), status.getMessage() + errorMessage, null);
				return ResponseEntity.ok(ares);
			}

			wlRequest.aggregate(true);
			nodes = promQ.getAppliablePromMetricNodesReadOnly(wlRequest);
		}

		for (PromMetricNode node : nodes) {
			double a = node.getScore();
			node.setBetFitScore(a);
			log.info("NodeScore: {} , cl: {}, node:[{}, {}]", a, node.getClUid(), node.getNode(), node.getUid());
		}

		return ResponseEntity.ok(nodes);
	}

	/**
	 * 워크로드 특성에 특정 설정에 해당하는 config name을 통해서 상세정보를 확인함
	 * 
	 * @param cfgName config name
	 * @return
	 * @throws Exception
	 */
	@GetMapping("/config/workloadfeature/{feature}")
	public ResponseEntity<?> getConfigGroup(@PathVariable("feature") CommonConfigGroup.ConfigName cfgname)
			throws Exception {
		CommonConfigGroup ccGroup = null;
		ccGroup = cmService.selectCommonConfigGroup(cfgname);

		List<Map> rsList = new ArrayList<Map>();
		JsonNode node = ccGroup.getContent();
		if (node.isArray()) {
			ArrayNode arrayNode = (ArrayNode) node; // JsonNode를 ArrayNode로 캐스팅

			// 배열의 각 요소를 순회
			Map map = null;
			String sName = "name";
			String sLevel = "level";
			String name;
			Integer level;
			for (JsonNode element : arrayNode) {
				name = (String) element.get(sName).asText();
				level = (Integer) (element.get(sLevel).asInt());

				map = new HashMap<String, Object>();
				map.put(sName, name);
				map.put(sLevel, level);
				rsList.add(map);
			}
		}

		APIResponseCode status = APIResponseCode.SUCCESS;
		APIResponse<List> response = new APIResponse(status.getCode(), status.getMessage(), rsList);

		return ResponseEntity.ok(response);
	}
}

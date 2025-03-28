package com.kware.policy.task.selector.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.kware.common.openapi.vo.APIResponse;
import com.kware.common.openapi.vo.APIResponseCode;
import com.kware.common.util.JSONUtil;
import com.kware.common.util.StringUtil;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;
import com.kware.policy.task.common.service.CommonService;
import com.kware.policy.task.common.service.vo.CommonConfigGroup;
import com.kware.policy.task.selector.service.WorkloadRequestService;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadResponse;
import com.kware.policy.task.selector.service.vo.WorkloadResponseStatus;

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
	
	// GET 방식의 testselect 엔드포인트: 요청 후 5초 후에 종료하는 샘플
    @GetMapping("/do/schedule/testselect")
    public ResponseEntity<String> doTestSelect() {
        try {
            // 5초 대기 (시뮬레이션)
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return ResponseEntity.ok("Test selection processed after 5 seconds.");
    }
    
    @Autowired
	private SimpMessagingTemplate messagingTemplate;
    
 // GET 방식의 testselect 엔드포인트: 요청 후 5초 후에 종료하는 샘플
    @GetMapping("/do/schedule/testwebsocketWorkload")
    public ResponseEntity<String> testwebsocketWorkload() {
        
    	messagingTemplate.convertAndSend("/topic/nodeSelectRequests", 1);
    	messagingTemplate.convertAndSend("/topic/newClusterWorkload", "kware-e36a5b34-aa71-43e1-b11a-b39822e87fc5");
    	/*
    	SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setContentType(org.springframework.util.MimeTypeUtils.TEXT_PLAIN);
        messagingTemplate.convertAndSend("/topic/newClusterWorkload",
                "kware-e36a5b34-aa71-43e1-b11a-b39822e87fc5",
                headerAccessor.getMessageHeaders());
    	*/
    	
        return ResponseEntity.ok("Test newClusterWorkload.");
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
		// WorkloadRequest wlRequest = YAMLUtil.read(requestString, WorkloadRequest.class);
		
		//if (log.isDebugEnabled())
		//	log.debug("배포요청 전문: {}", requestString);

		WorkloadRequest wlRequest = null;
		String errorMessage = null;
		try {
			wlRequest = JSONUtil.fromJsonToEmptyFromNull(requestString, WorkloadRequest.class);
		} catch (Exception e) {
			errorMessage = extractErrorMessage(e);
			if (errorMessage != null)
				log.error("Parser Error: {}", errorMessage, e);
			else {
				throw e;
			}
		}
		if (wlRequest == null) {
			status = APIResponseCode.INPUT_ERROR;
			APIResponse<String> ares = new APIResponse(status.getCode(), status.getMessage() + errorMessage, null);
			return ResponseEntity.ok(ares);
		}
		//wlRequest.aggregate(true);

		// {{ WorkloadRequest DB저장
		WorkloadRequest.Request req = wlRequest.getRequest();
		// 단순한 DB저장을 위해 원본 그대로를 json으로 변환하기 위함
		// req.setInfo(YAMLUtil.convertYamlToJson(requestString));
		req.setRequestJson(requestString);
//		req.setStatus(RequestStatus.request.toString());
		wlService.insertUserRequest(req);
		// }}
		
		this.cmService.createEvent("Workload Request", "Request"
				, "워크로드 요청 수신.\n" + wlRequest.getRequest().getMlId());

		//{{배포 대상 노드 셀렉터
		WorkloadResponse wlResponse = wlService.getResponseToSelectedNode(wlRequest);
		//}}

		// DB 저장
		wlService.insertMoUserResponse(wlResponse.getResponse());
		
		this.cmService.createEvent("Workload Response", "Request"
				, "워크로드 요청 노드 선택 결과 발신.\n" + wlRequest.getRequest().getMlId());

		// WorkloadRequest에 Response 입력
		//wlRequest.setResponse(wlResponse.getResponse());
		//String res_yamlstring = YAMLUtil.writeString(wlRequest, false);
		
		if(wlResponse.getResponse().getCode() == WorkloadResponseStatus.SUCCESS.getCode())
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
		
		this.cmService.createEvent("Workload Deploy Noti", "Request"
				, "워크로드 배포 완료 통지 수신.\n" + wlRequest.getRequest().getMlId());

		// {{ WorkloadRequest DB저장
		WorkloadRequest.Request req = wlRequest.getRequest();
		// 단순한 DB저장을 위해 원본 그대로를 json으로 변환하기 위함
		req.setNotiJson(requestString);
		
		wlService.updateUserRequest_noti(req);  //DB갱신
		wlService.scheduleNotiRequest(wlRequest); //기존 RequsetQ에서 기등록된 정보를 갱신
		// }}

		status = APIResponseCode.SUCCESS;
		APIResponse<String> ares = new APIResponse(status.getCode(), status.getMessage(), null);

		return ResponseEntity.ok(ares);
	}


	/**
	 * 워크로드 특성에 특정 설정에 해당하는 config name을 통해서 상세정보를 확인함
	 * 
	 * @param cfgName config name
	 * @return
	 * @throws Exception
	 */
	@GetMapping("/config/features/{feature}")
	public ResponseEntity<?> getConfigGroup(@PathVariable("feature") String feaName)
			throws Exception {
		List<CommonConfigGroup> ccGroups = null;
		ccGroups = cmService.getCommonConfigGroup(feaName);
		
		List<Map> rsList = new ArrayList<Map>();
		
		JsonNode node = null;
		String constant_name = "name";
		String constant_id = "id";
		String name, id;
		Map map = null;
		for(CommonConfigGroup ccGroup : ccGroups) {
			node = ccGroup.getContent();
			name = node.get(constant_name).asText();
			id   = node.get(constant_id).asText();
			
			map = new HashMap<String, Object>();
			map.put(constant_id, id);
			map.put(constant_name, name);
			rsList.add(map);
		}
		/*		
				if (node.isArray()) {
					ArrayNode arrayNode = (ArrayNode) node; // JsonNode를 ArrayNode로 캐스팅
		
					// 배열의 각 요소를 순회
					Map map = null;
					String sName = "name";
					String sLevel = "id";
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
		*/
		APIResponseCode status = APIResponseCode.SUCCESS;
		APIResponse<List> response = new APIResponse(status.getCode(), status.getMessage(), rsList);

		return ResponseEntity.ok(response);
	}
}

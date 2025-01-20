package com.kware.policy.task.common.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kware.common.openapi.vo.APIResponse;
import com.kware.common.openapi.vo.APIResponseCode;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.common.WorkloadCommandManager;
import com.kware.policy.task.common.service.CommonService;
import com.kware.policy.task.common.service.vo.CommonConfigGroup;
import com.kware.policy.task.common.service.vo.CommonEvent;
import com.kware.policy.task.common.vo.WorkloadCommand;
import com.kware.policy.task.feature.FeatureMain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 테스트를 위해서 진행함 1차 바로진행 2창 여기에서 rabbit mq로 보내고, rabbit mq에서 받아서 처리하고, mq에서 다시
 * 보내고, 받는 형태로 처리
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Slf4j
@RestController
@RequestMapping("/interface/common")
@RequiredArgsConstructor
public class CommonRestController {
	private final CommonService service;
	private final FeatureMain fm;

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

	// 추후 RabbitMQ에서 사용하도록 한다.
	/**
	 * 워크로드 특성에 특정 설정에 해당하는 config name을 통해서 상세정보를 확인함
	 * @param cfgName config name
	 * @return
	 * @throws Exception
	 */
	@GetMapping("/config/{cfgName}")//@PathVariable("val")
	public ResponseEntity<?> getConfigGroup(@PathVariable("cfgName") String cfgName) throws Exception {
		
		CommonConfigGroup.ConfigName cfgname= CommonConfigGroup.ConfigName.getConfigName(cfgName);
		CommonConfigGroup ccGroup = null;
		if(cfgname != null) {
			ccGroup = service.getCommonConfigGroup(cfgname);
			return ResponseEntity.ok(ccGroup);
		}else {
			return ResponseEntity.notFound()
					.header("X-Error-Reason", "Item not found")
                    .build();
			//return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process YAML");
		}
		
	}
	
	/**
	 * 시스템에 가지고 있는 워크로드 특성을 위한 config name의 리스트 제공
	 * @return
	 * @throws Exception
	 */
	@GetMapping("/config")//@PathVariable("val")
	public ResponseEntity<?> getConfigGroup() throws Exception {
		List cfgGroupList = service.getCommonConfigGroupList();
		return ResponseEntity.ok(cfgGroupList);
	}
	
	@GetMapping("/events/{id}")
	public List<CommonEvent> getAllEvents(@PathVariable long id) {
		CommonEvent event = new CommonEvent();
		event.setId(id);
		return service.getAllEvents(event);
	}

	@GetMapping("/events")
	public List<CommonEvent> getAllEvents() {
		return service.getAllEvents(null);
	}
	
	//프론트에서 설정이 변경되면 모든 설정을 DB에서 다시 읽어들이도록 한다.
	@GetMapping("/setting/change")
	public void changeSettings(HttpServletRequest request) {
		String gubun = request.getParameter("g");
		if("feature".equals(gubun)) {
			fm.init_workload_feature();
		}else if("node".equals(gubun)) {
			fm.init_node_feature();
		}else if("cluster".equals(gubun)) {
			fm.init_cluster_feature();
			fm.init_node_feature();
			
			WorkloadCommand<PromMetricPod> command = new WorkloadCommand<PromMetricPod>(WorkloadCommand.CMD_NODE_CHANGE,null);
			WorkloadCommandManager.addCommand(command);
		}
		log.debug("==============/setting/change?g=" + request.getParameter("g"));
		//service.getAllEvents(null);
	}
}

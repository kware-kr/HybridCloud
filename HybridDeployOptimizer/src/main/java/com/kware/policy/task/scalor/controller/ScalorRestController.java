package com.kware.policy.task.scalor.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kware.common.openapi.vo.APIResponse;
import com.kware.common.openapi.vo.APIResponseCode;
import com.kware.policy.task.common.service.CommonService;
import com.kware.policy.task.feature.FeatureMain;
import com.kware.policy.task.scalor.service.ScalingInfoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 테스트를 위해서 진행함 1차 바로진행 2창 여기에서 rabbit mq로 보내고, rabbit mq에서 받아서 처리하고, mq에서 다시
 * 보내고, 받는 형태로 처리
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Slf4j
@RestController
@RequestMapping("/interface/scalor")
@RequiredArgsConstructor
public class ScalorRestController {
	private final CommonService comService;
	private final FeatureMain fm;
	private final ScalingInfoService scalingService;

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


	/**
	 * 노드 스케일링 요청에  callback함수 => 현재 미결
	 * @param msg
	 * @return
	 * @throws Exception
	 */
	@GetMapping("/cluster/nodescale/callback")
	public ResponseEntity<?> nodeScalingCallback(@RequestBody(required = false) String msg) {
		log.debug("==============/interface/scalor/cluster/nodescale/callback" + msg);
		scalingService.processNodeScalingCallback(msg);
		//service.getAllEvents(null);
		
		return ResponseEntity.ok().build();
	}
}

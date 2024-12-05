package com.kware.hybrid.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kware.common.util.JSONUtil;
import com.kware.hybrid.service.WorkloadRequestService;
import com.kware.hybrid.service.vo.WorkloadRequestVO;

@RestController
@RequestMapping("/workload")
public class WorkloadRestController {

	final private WorkloadRequestService wrService;

	WorkloadRestController(WorkloadRequestService service) {
		this.wrService = service;
	}

	@RequestMapping("/request")
	public ResponseEntity<Object> getWorkloadRequest(@RequestBody(required = false) String paramBody) {
		List<WorkloadRequestVO> resultList = null;
		
		if (paramBody != null && !paramBody.isEmpty()) {
			Map map = JSONUtil.getMapFromJsonString(paramBody);
			
			String mlId = (String)map.get("mlId");
			if(mlId == null) {
				resultList = wrService.getRunningWrokload();
				if (resultList != null) {
					return ResponseEntity.ok(resultList);
				}
			}
			else {
				WorkloadRequestVO obj = (WorkloadRequestVO)wrService.getRunningWrokloadByMlid(mlId);
				if (obj != null) {
					resultList = new ArrayList<WorkloadRequestVO>();
					resultList.add(obj);
					return ResponseEntity.ok(resultList);
				}
			}			
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Data not found");
	}
}

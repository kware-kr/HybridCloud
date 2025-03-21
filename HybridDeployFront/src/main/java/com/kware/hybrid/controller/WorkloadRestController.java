package com.kware.hybrid.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kware.common.openapi.vo.APIPagedResponse;
import com.kware.common.openapi.vo.APIResponseCode;
import com.kware.hybrid.service.WorkloadRequestService;
import com.kware.hybrid.service.vo.ResourcePodUsageVO;
import com.kware.hybrid.service.vo.WorkloadRequestVO;

@RestController
@RequestMapping("/workload")
public class WorkloadRestController {

	final private WorkloadRequestService wrService;

	WorkloadRestController(WorkloadRequestService service) {
		this.wrService = service;
	}

	@RequestMapping("/request")
	public ResponseEntity<Object> getWorkloadRequest(@RequestBody(required = false) WorkloadRequestVO apReq) {
		List<WorkloadRequestVO> resultList = null;
		
		if(apReq == null) {
			apReq = new WorkloadRequestVO();
			apReq.setDefautPage10();
		}
			
		String mlId = apReq.getMlId();
		if(mlId == null) {
			resultList = wrService.getRunningWrokload(apReq);
			
			int totalElements = apReq.getTotalElements(); //클라이언트가 보내주면
			if(apReq.getPageNumber() == 1)
				totalElements = wrService.getRunningWrokloadCount(apReq);
			
			if (resultList != null) {
				APIResponseCode arCode = APIResponseCode.SUCCESS; 
				APIPagedResponse<WorkloadRequestVO> apResponse = new APIPagedResponse<WorkloadRequestVO>(arCode.getCode(), arCode.getMessage()
						, resultList, apReq.getPageNumber(), apReq.getPageSize(), totalElements);
				
				return ResponseEntity.ok(apResponse);
			}
		}else {
			WorkloadRequestVO obj = (WorkloadRequestVO)wrService.getRunningWrokloadByMlid(mlId);
			if (obj != null) {
				resultList = new ArrayList<WorkloadRequestVO>();
				resultList.add(obj);
				
				APIResponseCode arCode = APIResponseCode.SUCCESS; 
				APIPagedResponse<WorkloadRequestVO> apResponse = new APIPagedResponse<WorkloadRequestVO>(arCode.getCode(), arCode.getMessage()
						, resultList , apReq.getPageNumber(), apReq.getPageSize(), 1);
				
				return ResponseEntity.ok(apResponse);
			}
		}			
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Data not found");
	}
	
	@RequestMapping("/podusage")
	public ResponseEntity<Object> getWorkloadPodUsage(@RequestBody ResourcePodUsageVO apReq) {
		List<ResourcePodUsageVO> resultList = null;
			
		String mlId = apReq.getMlId();
		String podUid = apReq.getPodUid();
		if(mlId != null && podUid != null ) {
			resultList = wrService.getPodUsage(apReq);
			
			if (resultList != null) {
				APIResponseCode arCode = APIResponseCode.SUCCESS; 
				APIPagedResponse<ResourcePodUsageVO> apResponse = new APIPagedResponse<ResourcePodUsageVO>(arCode.getCode(), arCode.getMessage()
						, resultList);
				
				return ResponseEntity.ok(apResponse);
			}
		}else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body("Invalid request: mlId and podUid cannot be null or empty");
		}			
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Data not found");
	}	
}


/*
 * if(apReq == null) {
			apReq = new ResourceUsageNode();
			apReq.setDefautPage10();
		}
		List list = ruService.selectResourceUsageNodeList(apReq);
		list = convertToMapFromJsonstring(list);
		
		APIResponseCode arCode = APIResponseCode.SUCCESS; 
		APIPagedResponse apResponse = new APIPagedResponse<Map>(arCode.getCode(), arCode.getMessage(), list, apReq.getPageNumber(), apReq.getPageSize());
		return ResponseEntity.ok(apResponse);
 **/

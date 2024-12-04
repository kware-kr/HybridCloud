package com.kware.hybrid.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kware.common.util.JSONUtil;
import com.kware.hybrid.service.CommonFeatureService;
import com.kware.hybrid.service.vo.ClusterNodeFeatureVO;
import com.kware.hybrid.service.vo.CommonFeatureVO;

@RestController
@RequestMapping("/setting")
public class SettingRestController {

	final private CommonFeatureService cfService;

	SettingRestController(CommonFeatureService service) {
		this.cfService = service;
	}
	
	@RequestMapping("/clusterfeature")
	public ResponseEntity<Object> clusterresoruce(@RequestBody(required = false) String paramBody, HttpMethod method) {
		 
		switch (method) {
			case GET:
				// GET 요청: 조회
				List<ClusterNodeFeatureVO> resultList  = cfService.getAllClusterFeatures();
				if (resultList != null) {
					return ResponseEntity.ok(resultList);
				}
								
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Data not found");
			case PUT:
				// PUT 요청: 아이템 갱신
				if (paramBody != null && !paramBody.isEmpty()) {
					ClusterNodeFeatureVO vo = null;
					try {
						vo = JSONUtil.fromJsonToEmptyFromNull(paramBody, ClusterNodeFeatureVO.class);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					cfService.updateClusterFeature(vo);
					return ResponseEntity.status(HttpStatus.OK).build();
				} else {
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Data not found to update");
				}
			default:
				return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Invalid method");
		}
	}
	
	@RequestMapping("/clusternodefeature")
	public ResponseEntity<Object> clusternoderesoruce(@RequestBody(required = false) String paramBody, HttpMethod method) {
		 
		switch (method) {
			case GET:
				// GET 요청: 조회
				List<ClusterNodeFeatureVO> resultList  = cfService.getAllClusterNodeFeatures();
				if (resultList != null) {
					return ResponseEntity.ok(resultList);
				}
								
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Data not found");
			case PUT:
				// PUT 요청: 아이템 갱신
				if (paramBody != null && !paramBody.isEmpty()) {
					ClusterNodeFeatureVO vo = null;
					try {
						vo = JSONUtil.fromJsonToEmptyFromNull(paramBody, ClusterNodeFeatureVO.class);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					cfService.updateClusterNodeFeature(vo);
					return ResponseEntity.status(HttpStatus.OK).build();
				} else {
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Data not found to update");
				}
			default:
				return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Invalid method");
		}
	}

	@RequestMapping("/{settingtype}")
	public ResponseEntity<Object> commonresoruce(@PathVariable("settingtype") String settingType, @RequestBody(required = false) String paramBody, HttpMethod method) {
		String pkey = null;
		String skey = null;
		settingType = settingType.toLowerCase();
		 switch (settingType) {
	            case "minresource":
	            	pkey = "min_resource_capacity";
	            	break;
	            case "podscaling":
	            	pkey = "pod_scaling_policies";
	            	break;
	            case "nodescaling":
	            	pkey = "node_scaling_policies";
	            	break;
	            case "workloadfeature":
	            	if(paramBody != null) {
		            	Map map = JSONUtil.getMapFromJsonString(paramBody);
		            	skey = map.get("id").toString();
		            	map.clear();
	            	}
	            	pkey = "workload_feature";
	            	break;
	            default:
	                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request uri");
	        }
		 
		switch (method) {
			case GET:
				// GET 요청: 조회
				
				if(skey != null) {
					CommonFeatureVO result = cfService.getCommonFeatureByKey(pkey, skey);
					if (result != null) {
						return ResponseEntity.ok(result.getFeaContent());
					}
				}else {
					List<CommonFeatureVO> resultList  = cfService.getAllCommonFeatures(pkey);
					if (resultList != null) {
						List<String> feaContentList = new ArrayList<>();  // 새로운 리스트 생성
	
						// resultList에서 각 요소를 순회하며 getFeaContent() 호출
						for (CommonFeatureVO feature : resultList) {
						    feaContentList.add(feature.getFeaContent());  // getFeaContent() 값을 feaContentList에 추가
						}
						resultList.clear();
						resultList = null;
						return ResponseEntity.ok(feaContentList);
					}
				}				
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Data not found");

			case POST:
				if(!settingType.equals("workloadfeature")) {
					skey = Long.toString(System.nanoTime());
				}
				// POST 요청: 새로운 아이템 추가
				if (paramBody != null && !paramBody.isEmpty()) {
					CommonFeatureVO vo = new CommonFeatureVO();
					vo.setFeaName(pkey);
					vo.setFeaSubName(skey);
					vo.setFeaContent(paramBody);
					try {
						cfService.insertCommonFeature(vo);
					}catch(DataIntegrityViolationException e) {
						return ResponseEntity.status(HttpStatus.CONFLICT).body("Resource already exists");
					}
					
					if(!settingType.equals("workloadfeature")) {
					    String jsonString = "{\"id\":\"" + skey + "\"}";
						return ResponseEntity.status(HttpStatus.CREATED).body(jsonString);
					}
					
					return ResponseEntity.status(HttpStatus.CREATED).build();
				} else {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid data");
				}

			case PUT:
				// PUT 요청: 아이템 갱신
				if (paramBody != null && !paramBody.isEmpty()) {
					CommonFeatureVO vo = new CommonFeatureVO();
					vo.setFeaName(pkey);
					vo.setFeaSubName(skey);
					vo.setFeaContent(paramBody);
					cfService.updateCommonFeature(vo);
					return ResponseEntity.status(HttpStatus.OK).build();
				} else {
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Data not found to update");
				}

			case DELETE: //workloadfeature만 처리함
				if(!settingType.equals("workloadfeature")) {
					return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
				}
				
				
				if (paramBody != null && !paramBody.isEmpty()) {
					int count = cfService.deleteCommonFeature(pkey, skey);
					 String resultString = null;
					if(count == 1) { //임시로 하고 springboot에서 제공하는 것으로 수정
						 resultString = "{\"success\":" + true + "}";
					}else {
						resultString = "{\"success\":" + false + "}";
					}
					
					return ResponseEntity.ok().body(resultString);
				} else {
					//throw new IllegalArgumentException("RequestBody is empty.");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("RequestBody is empty");
				}

			default:
				return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Invalid method");
		}
	}	
}

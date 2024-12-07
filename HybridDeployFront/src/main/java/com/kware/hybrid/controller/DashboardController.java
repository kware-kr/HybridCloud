package com.kware.hybrid.controller;

import java.util.HashMap;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kware.hybrid.service.DashboardService;
import com.kware.hybrid.service.vo.DashboardVO;

@RestController
//@RequestMapping("/dashboard")
public class DashboardController {

	final private DashboardService dbService;

	DashboardController(DashboardService service) {
		this.dbService = service;
	}

	@GetMapping("/dashboard")
	public ResponseEntity<Object> getDashboard() {
		
		List<DashboardVO> dashsboardList = dbService.getDashboard();
		List<DashboardVO> clusterList = dbService.getClusters();
		
		HashMap<String, Object> hmap = new HashMap<String, Object>();
		
		hmap.put("pods"     , dashsboardList);
		hmap.put("clusters" , clusterList);
		
		return ResponseEntity.ok(hmap);

	}
}

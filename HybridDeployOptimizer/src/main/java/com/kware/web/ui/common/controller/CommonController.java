package com.kware.web.ui.common.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 모든 UI관련 처리를 담당한다.
 */
@Controller
//@RequestMapping("")
public class CommonController {
	static Logger logger = LoggerFactory.getLogger(CommonController.class);

	@RequestMapping("/")
	public String ScheduleManagementRoot() throws Exception{
		return "index";
	}
	
	final int ext_length = ".ui".length();
	@GetMapping("/**/*.ui")
	public String getUiRequest(HttpServletRequest request){
		/* 참고사항
		 * IDE에서는 /aaa.html 형태로 리턴해도 잘되지만 실제 jar를 이용해서 처리하면 타임리프 엔진이 
		 * 실제 경로를 찾지못하여 오류를 발생한다. 그래서 aaa.html, aa/aaa.html이런형태로 지정하면 정상동작한다.
		 * 즉 절대경로를 지정하면 경로를 찾지못한다.
		 * 해결책: aaa.html ./aaa.html 즉 여기는 파일시스템 경로를 의미한다.  
		 */
		String uri = request.getRequestURI();
		uri = uri.substring(1, uri.length() - ext_length);
		System.out.println(uri);
		return uri;
	}
}

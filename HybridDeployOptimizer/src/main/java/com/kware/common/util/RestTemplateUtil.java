package com.kware.common.util;

import org.slf4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestTemplateUtil {

    private final RestTemplate restTemplate;

    public RestTemplateUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 공통 HTTP 요청 메서드
     * 
     * @param url       요청할 URL
     * @param method    HTTP 메서드 (GET, POST, PUT, DELETE)
     * @param body      요청 본문 (POST, PUT의 경우)
     * @param headers   요청 헤더 (null 가능)
     * @param responseType 응답 타입 (예: String.class, Map.class 등)
     * @return 응답 객체
     */
    public <T> ResponseEntity<T> sendRequest(String url, HttpMethod method, Object body, 
    		HttpHeaders httpHeaders, Class<T> responseType , Logger _log) {
    	Logger llog = _log;
    	if(llog == null)
    		llog = this.log;
      /*  
        HttpEntity<Object> entity = new HttpEntity<>(body, httpHeaders);

        return restTemplate.exchange(url, method, entity, responseType);
        */
    	if(llog.isInfoEnabled())
    		llog.info("Sending {} request to URL: {}", method, url);
        if (httpHeaders != null) {
        	if(llog.isDebugEnabled())
        		llog.debug("Request Headers: {}", httpHeaders);
        }
        if (body != null) {
        	if(llog.isDebugEnabled())
        		llog.debug("Request Body: {}", body);
        }

        HttpEntity<Object> entity = new HttpEntity<>(body, httpHeaders);
        ResponseEntity<T> response = restTemplate.exchange(url, method, entity, responseType);

        if(llog.isInfoEnabled())
        	llog.info("Received response with status: {}", response.getStatusCode());
        
        if (response.getBody() != null) {
        	if(llog.isDebugEnabled())
        		llog.debug("Response Body: {}", response.getBody());
        }
        return response;
    }
    
    public <T> ResponseEntity<T> sendRequest(String url, HttpMethod method, Object body, 
    		HttpHeaders httpHeaders, Class<T> responseType) {
    	
    	return sendRequest(url, method, body, httpHeaders, responseType, this.log);
    	
    }

    /**
     * GET 요청
     */
    public <T> ResponseEntity<T> get(String url, Object body, HttpHeaders headers, Class<T> responseType) {
        return sendRequest(url, HttpMethod.GET, body, headers, responseType);
    }
    
    public <T> ResponseEntity<T> get(String url, Object body, HttpHeaders headers, Class<T> responseType, Logger _log) {
        return sendRequest(url, HttpMethod.GET, body, headers, responseType, _log);
    }


    /**
     * POST 요청
     */
    public <T> ResponseEntity<T> post(String url, Object body, HttpHeaders headers, Class<T> responseType) {
        return sendRequest(url, HttpMethod.POST, body, headers, responseType);
    }
    
    public <T> ResponseEntity<T> post(String url, Object body, HttpHeaders headers, Class<T> responseType, Logger _log) {
        return sendRequest(url, HttpMethod.POST, body, headers, responseType, _log);
    }

    /**
     * PUT 요청
     */
    public <T> ResponseEntity<T> put(String url, Object body, HttpHeaders headers, Class<T> responseType) {
        return sendRequest(url, HttpMethod.PUT, body, headers, responseType);
    }
    
    public <T> ResponseEntity<T> put(String url, Object body, HttpHeaders headers, Class<T> responseType, Logger _log) {
        return sendRequest(url, HttpMethod.PUT, body, headers, responseType, _log);
    }

    /**
     * DELETE 요청
     */
    public <T> ResponseEntity<T> delete(String url, HttpHeaders headers, Class<T> responseType) {
        return sendRequest(url, HttpMethod.DELETE, null, headers, responseType);
    }
    
    public <T> ResponseEntity<T> delete(String url, HttpHeaders headers, Class<T> responseType, Logger _log) {
        return sendRequest(url, HttpMethod.DELETE, null, headers, responseType, _log);
    }
}
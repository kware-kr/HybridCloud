package com.kware.common.util;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

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
                                             Map<String, String> headers, Class<T> responseType) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::set);
        }

        HttpEntity<Object> entity = new HttpEntity<>(body, httpHeaders);

        return restTemplate.exchange(url, method, entity, responseType);
    }

    /**
     * GET 요청
     */
    public <T> ResponseEntity<T> get(String url, Map<String, String> headers, Class<T> responseType) {
        return sendRequest(url, HttpMethod.GET, null, headers, responseType);
    }

    /**
     * POST 요청
     */
    public <T> ResponseEntity<T> post(String url, Object body, Map<String, String> headers, Class<T> responseType) {
        return sendRequest(url, HttpMethod.POST, body, headers, responseType);
    }

    /**
     * PUT 요청
     */
    public <T> ResponseEntity<T> put(String url, Object body, Map<String, String> headers, Class<T> responseType) {
        return sendRequest(url, HttpMethod.PUT, body, headers, responseType);
    }

    /**
     * DELETE 요청
     */
    public <T> ResponseEntity<T> delete(String url, Map<String, String> headers, Class<T> responseType) {
        return sendRequest(url, HttpMethod.DELETE, null, headers, responseType);
    }
}
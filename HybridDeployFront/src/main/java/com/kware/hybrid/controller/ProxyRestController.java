package com.kware.hybrid.controller;



import java.util.Base64;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/proxy")
public class ProxyRestController {

    private final RestTemplate restTemplate;

    @Value("${hybrid.proxy.url:null}")
    private String targetUrl;

    public ProxyRestController() {
        this.restTemplate = new RestTemplate();
    }

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<?> proxyRequest(
    		HttpServletRequest request,  // HTTP 요청 객체 사용
            @RequestHeader Map<String, String> headers,
            @RequestBody(required = false) String body,
            HttpMethod method,
            @RequestParam Map<String, String> params,
            @RequestHeader(value = "Accept", required = false) String accept,
            @RequestHeader(value = "Content-Type", required = false) String contentType
    ) {
    	
    	String uri = request.getRequestURI();  // 예: /proxy/somePath/123
        
        // Construct target URL
        String url = targetUrl + uri.replace("/proxy", "") +  (params.isEmpty() ? "" : "?" + params.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse(""));

        // Build headers for the request
        HttpHeaders proxyHeaders = new HttpHeaders();
        headers.forEach(proxyHeaders::add);
 /*       
        String username = "admin";
        String password = "admin123!";

        // Basic Authentication: username:password를 Base64로 인코딩
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        
        headers.put("Authorization", "Basic " + encodedAuth);
*/
        // Build request entity
        HttpEntity<String> requestEntity = new HttpEntity<>(body, proxyHeaders);

        // Forward the request to the target URL
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                method,
                requestEntity,
                String.class
        );

        // Return the response back to the client
        return ResponseEntity
                .status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(response.getBody());
    }
}


package com.kware.hybrid.controller;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

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
            @RequestHeader Map<String, String> headers,
            @RequestBody(required = false) String body,
            HttpMethod method,
            @RequestParam Map<String, String> params,
            @RequestHeader(value = "Accept", required = false) String accept,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            @PathVariable Map<String, String> pathVars
    ) {
        // Construct target URL
        String url = targetUrl + pathVars.getOrDefault("path", "") + (params.isEmpty() ? "" : "?" + params.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse(""));

        // Build headers for the request
        HttpHeaders proxyHeaders = new HttpHeaders();
        headers.forEach(proxyHeaders::add);

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


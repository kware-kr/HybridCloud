package com.kware.hybrid.websocket;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//BackendWebSocketClient.java

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BackendWebSocketClient {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${hybrid.proxy.url:null}")
    private String targetUrl;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private SockJsClient sockJsClient;
    private WebSocketStompClient stompClient;
    private String backendUrl;

    @PostConstruct
    public void init() {
        // SockJS 클라이언트 초기화
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        sockJsClient = new SockJsClient(transports);
        
        // WebSocketStompClient 생성 및 메시지 컨버터 설정
        stompClient = new WebSocketStompClient(sockJsClient);
        
        // 복합 메시지 컨버터 구성: 문자열 메시지와 JSON 메시지를 모두 지원
        List<org.springframework.messaging.converter.MessageConverter> converters = new ArrayList<>();
        converters.add(new org.springframework.messaging.converter.StringMessageConverter());
        converters.add(new org.springframework.messaging.converter.MappingJackson2MessageConverter());
        stompClient.setMessageConverter(new org.springframework.messaging.converter.CompositeMessageConverter(converters));

        // 백엔드 URL 구성
        if (targetUrl != null) {
            backendUrl = targetUrl + "/interface/ws"; // 예: http://172.30.1.28:8889/interface/ws
            connect();
        } else {
            log.warn("Target URL is null. Cannot connect to backend WebSocket.");
        }
    }

    private void connect() {
        // 재연결 시 targetUrl이 유효한지 확인
        if (targetUrl == null) {
            log.warn("Target URL is null. Skipping connection attempt.");
            return;
        }

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                log.info("Connected to backend WebSocket: {}", connectedHeaders);
               
                // 두 번째 토픽 구독 추가: 서버에 요청이 등록되어 서버에서 수집이 이루어진 경우, 즉 수집이 되면
                session.subscribe("/topic/newClusterWorkload", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                    	//String contentType = headers.getFirst("content-type");
                        //log.debug("newClusterWorkload 구독 메시지의 Content-Type: {}", contentType);
                        return String.class;
                    }
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        if (log.isDebugEnabled()) {
                            log.debug("Received WebSocket message from /topic/newClusterWorkload: {}", payload);
                        }
                        messagingTemplate.convertAndSend("/topic-proxy/newClusterWorkload", payload);
                    }
                });
                
                // 백엔드에서 /topic/nodeSelectRequests 토픽의 메시지를 구독;; 사용자가 요청한 경우
                session.subscribe("/topic/nodeSelectRequests", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                    	//String contentType = headers.getFirst("content-type");
                        //log.debug("nodeSelectRequests 구독 메시지의 Content-Type: {}", contentType);
                        return String.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        if (log.isDebugEnabled()) {
                            log.debug("Received WebSocket message: {}", payload);
                        }
                        messagingTemplate.convertAndSend("/topic-proxy/nodeSelectRequests", payload);
                    }
                });
                
               
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                log.error("Transport error in WebSocket connection", exception);
                // 재연결 시도
                reconnect();
            }
        };

        log.info("Attempting to connect to backend WebSocket at {}", backendUrl);
        stompClient.connect(backendUrl, sessionHandler);
    }

    private void reconnect() {
        log.info("Scheduling reconnection in 5 seconds...");
        scheduler.schedule(() -> {
            log.info("Attempting to reconnect to backend WebSocket...");
            connect();
        }, 20, TimeUnit.SECONDS);
    }
}


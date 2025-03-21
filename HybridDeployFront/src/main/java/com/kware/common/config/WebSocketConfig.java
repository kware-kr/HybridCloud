package com.kware.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
   @Override
   public void registerStompEndpoints(StompEndpointRegistry registry) {
      // 클라이언트가 연결할 엔드포인트 (SockJS 지원)
      registry.addEndpoint("/ws-proxy")
             // .setAllowedOrigins("*")
              .withSockJS();
   }

   @Override
   public void configureMessageBroker(MessageBrokerRegistry registry) {
      // 클라이언트가 구독할 토픽 및 애플리케이션 접두사 설정
	   registry.enableSimpleBroker("/topic-proxy");
       registry.setApplicationDestinationPrefixes("/app-proxy");
   }
}

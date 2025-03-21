package com.kware.policy.task.selector.filter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class NodeSelectRequestCounterFilter extends OncePerRequestFilter {

   private final AtomicInteger concurrentRequests = new AtomicInteger(0);

   @Autowired
   private SimpMessagingTemplate messagingTemplate;

   // 필터를 적용할 대상: POST /do/schedule/nodeselect
   @Override
   protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
	   String method = request.getMethod();
       String uri = request.getRequestURI();
       return !(("POST".equalsIgnoreCase(method) && "/interface/api/v1/do/schedule/nodeselect".equals(uri))
             || ("GET".equalsIgnoreCase(method) && "/interface/api/v1/do/schedule/testselect".equals(uri)));
   }

   @Override
   protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
      try {
         // 요청 시작 시 카운터 증가 및 WebSocket 전송
         int count = concurrentRequests.incrementAndGet();
         messagingTemplate.convertAndSend("/topic/nodeSelectRequests", count);

         filterChain.doFilter(request, response);
      } finally {
         // 응답 후 카운터 감소 및 WebSocket 전송
         int count = concurrentRequests.decrementAndGet();
         messagingTemplate.convertAndSend("/topic/nodeSelectRequests", count);
      }
   }
}

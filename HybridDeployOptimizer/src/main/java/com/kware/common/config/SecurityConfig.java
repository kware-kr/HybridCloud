package com.kware.common.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

	/**
	 * spring security 5.7.1에서 authorizeHttpRequests는 많은 변화가 있네. authorizeRequests를 사용하는게 더 나을려나....
	 * @param http
	 * @return
	 * @throws Exception
	 */
	@Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		 RequestMatcher interfaceMatcher = new AntPathRequestMatcher("/interface/**");
	     RequestMatcher actuatorMatcher  = new AntPathRequestMatcher("/actuator/**");
	     RequestMatcher queueMatcher     = new AntPathRequestMatcher("/queue/**");

	     http
            .csrf(csrf -> csrf.disable())                               //CSRF 비활성화
            .authorizeHttpRequests(authz -> authz
                            //.requestMatchers(interfaceMatcher).authenticated()      //   /interface/** 엔드포인트는 인증 필요
                            .requestMatchers(interfaceMatcher).access(interfaceAccessManager)      //   /interface/** 엔드포인트는 인증 필요
                            .requestMatchers(actuatorMatcher, queueMatcher).access(actuatorAccessManager)
                            .anyRequest().denyAll()                               // 그 외의 모든 요청은 허용
            )
            .sessionManagement(session -> session
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션을 사용하지 않으므로 STATELESS 설정
            )
            .httpBasic(withDefaults()); // HTTP Basic 인증 사용

    return http.build();
    }
	
	private static AuthorizationManager<RequestAuthorizationContext> hasIpAddress(String ipAddress) {
        IpAddressMatcher ipAddressMatcher = new IpAddressMatcher(ipAddress);
        return (authentication, context) -> {
            HttpServletRequest request = context.getRequest();
            return new AuthorizationDecision(ipAddressMatcher.matches(request));
        };
    }
	
	// AuthorizationManager for /interface/**
    private AuthorizationManager<RequestAuthorizationContext> interfaceAccessManager = (authc, context) -> {
        Authentication authentication = authc.get(); // 현재 요청의 인증 정보
        // 인증이 되어 있으면 접근 허용
        return new AuthorizationDecision(authentication != null);
    };
    
    //String permitAddress[] = {"183.109.110.211", "172.30.1"};
    
    @Value("${spring.security.permit-addresses:}")
    private String[] permitAddresses;
    
    private List<String> finalPermitAddresses;
    
    @PostConstruct
    public void init() {
        // 기본값 127.0.0.1을 포함하도록 배열 처리
        finalPermitAddresses = new ArrayList<>(Arrays.asList(permitAddresses));
        if (!finalPermitAddresses.contains("127.0.0.1")) {
            finalPermitAddresses.add("127.0.0.1");
        }
    }

    // AuthorizationManager for /actuator/** 
    //허용된 IP 이거나, 허용된 사용자일 경우에 통과
    private AuthorizationManager<RequestAuthorizationContext> actuatorAccessManager = (authc, context) -> {
        String ipAddress = context.getRequest().getRemoteAddr(); // 요청의 IP 주소
        //boolean hasValidIp = ipAddress.equals("172.30.1.251") || ipAddress.equals("172.30.1.28");
        boolean hasValidIp = false; 
        
        for (String permitAddress : finalPermitAddresses) {
            hasValidIp = ipAddress.startsWith(permitAddress);
        	if(hasValidIp) {
        		break;
        	}
        }
        
        if(log.isDebugEnabled())
        	log.debug("Has ROLE_IP: {},{}", ipAddress, hasValidIp);
        
        if(hasValidIp)
        	return new AuthorizationDecision(hasValidIp);
        
        Authentication authentication = authc.get();
        boolean hasRoleActuator = authentication.getAuthorities().stream()
                .peek(authority -> log.debug("Checking authority: {}", authority.getAuthority()))
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ACTUATOR"));
        
        if(log.isDebugEnabled())
        	log.debug("Has ROLE_ACTUATOR: {}", hasRoleActuator);
        
        // IP 주소와 역할 조건을 모두 만족해야 접근 허용
        return new AuthorizationDecision(hasRoleActuator);
    };
}
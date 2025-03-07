package com.kware.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	/**
	 * 	Default
	 	Allow all origins.
		Allow "simple" methods GET, HEAD and POST.
		Allow all headers.
		Set max age to 1800 seconds (30 minutes).
	 */
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
		.allowedOrigins("*")
        //.allowedMethods("GET", "POST")
		//.maxAge(3000)
		;
		
		WebMvcConfigurer.super.addCorsMappings(registry);
	}
	
	@Bean
    public RestTemplate restTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 연결 타임아웃 5초
        factory.setReadTimeout(10000);    // 응답(읽기) 타임아웃 5초

        return new RestTemplate(factory);
    }
}

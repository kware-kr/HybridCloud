package com.kware.common.config;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

	// 전체 API 그룹
	@Bean
	public GroupedOpenApi publicApi() {
		return GroupedOpenApi.builder().group("default")
				.pathsToMatch("/queue/**", "/interface/**") // "/api"로 시작하는 경로만 포함
				.build();
	}

	// 추가적인 정보 설정
	@Bean
	public io.swagger.v3.oas.models.OpenAPI customOpenAPI() {
		return new io.swagger.v3.oas.models.OpenAPI()
				.info(new Info().title("HybridOptimizer Rest API").version("1.0").description("하이브리드 API 예제 설정"))
				//.addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
				//.components(new io.swagger.v3.oas.models.Components().addSecuritySchemes("BearerAuth",
				//		new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
				;
	}
}
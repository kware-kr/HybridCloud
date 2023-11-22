package kware.common.config;

import cetus.config.CetusConfig;
import cetus.log.LoggingInterceptor;
import cetus.menu.MenuInterceptor;
import cetus.user.SessionUserInterceptor;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.resource.VersionResourceResolver;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final CetusConfig configs;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
                .resourceChain(true)
                .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));
    }

    @Bean
    public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
        return new ResourceUrlEncodingFilter();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        loggingInterceptor().setLogger(LoggerFactory.getLogger(LoggingInterceptor.class));
        loggingInterceptor().setLogging(configs.getLogging());
        registry.addInterceptor(loggingInterceptor()).excludePathPatterns("/assets/**/*", "/assets/**/*");
        registry.addInterceptor(userInterceptor()).excludePathPatterns("/assets/**/*", "/assets/**/*", "/summernote-0.8.18/**/*", "/login", "/loginProc", "/logout", "/error/**/*");
        registry.addInterceptor(menuInterceptor()).excludePathPatterns("/assets/**/*", "/assets/**/*", "/summernote-0.8.18/**/*", "/login", "/loginProc", "/logout", "/error/**/*", "/**/**/*.json", "/board/myDownload");
    }

    @Bean
    public LoggingInterceptor loggingInterceptor() {
        return new LoggingInterceptor();
    }

    @Bean
    public SessionUserInterceptor userInterceptor() {
        return new SessionUserInterceptor();
    }

    @Bean
    public MenuInterceptor menuInterceptor() {
        return new MenuInterceptor();
    }
}

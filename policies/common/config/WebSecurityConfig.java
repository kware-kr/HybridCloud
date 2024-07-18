package kware.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class WebSecurityConfig {

    private final ApplicationContext applicationContext;

    //    @Bean
//    public BCryptPasswordEncoder encodePwd() {
//        return new BCryptPasswordEncoder();
//    }
    @Bean
    public TestPasswordEncoder encoder() {
        return new TestPasswordEncoder();
    }

    // HTTP 관련 인증 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        List<String> profiles = Arrays.asList(applicationContext.getEnvironment().getActiveProfiles());
        boolean isDev = profiles.contains("dev") || profiles.contains("local");

        if (isDev) {
            httpSecurity.csrf().disable().cors().disable().headers().disable();
        } else {
            httpSecurity.csrf().disable()
//                .csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                .and() //
                    .cors().configurationSource(corsConfigurationSource()).and().headers().frameOptions(frameOptionsConfig -> {
                        frameOptionsConfig.sameOrigin().xssProtection(xXssConfig -> xXssConfig.xssProtectionEnabled(true));
                    });
        }

        httpSecurity.authorizeRequests(expressionInterceptUrlRegistry -> {
                    expressionInterceptUrlRegistry.antMatchers("/login", "/assets/**").permitAll().antMatchers("/**").authenticated() // 누구나 접근 가능
                            .anyRequest().authenticated() // 나머지는 권한이 있기만 하면 접근 가능
                    ;
                }).formLogin(httpSecurityFormLoginConfigurer -> {
                    httpSecurityFormLoginConfigurer.loginPage("/login") // 로그인 페이지 링크
                            .usernameParameter("userId").loginProcessingUrl("/loginProc").defaultSuccessUrl("/", true) // 로그인 성공시 연결되는 주소
                    ;
                }) // 로그인 설정
                .logout(httpSecurityLogoutConfigurer -> {
                    httpSecurityLogoutConfigurer.logoutUrl("/logout").logoutSuccessUrl("/login") // 로그아웃 성공시 연결되는 주소
                            .invalidateHttpSession(true) // 로그아웃시 저장해 둔 세션 제거
                    ;
                }) // 로그아웃 설정
        ;

        return httpSecurity.build();
    }

    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*");
        configuration.addAllowedHeader("Origin, X-Requested-With, Content-Type, Accept, Authorization, x-csrf-token");
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}

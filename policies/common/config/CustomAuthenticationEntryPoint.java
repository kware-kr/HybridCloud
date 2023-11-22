package kware.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Slf4j
public class CustomAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
    public CustomAuthenticationEntryPoint(String loginUrl) {
        super(loginUrl);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {

        log.info("CustomAuthenticationEntryPoint::commence");
        String ajaxHeader = request.getHeader("X-Requested-With");

        // AJAX 요청인지 검사 (헤더 검사, 비동기인지 체크)
        boolean isAjax = "XMLHttpRequest".equals(ajaxHeader);
        if (isAjax) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "세션 만료로 인해서 거부되었습니다.");
        } else {
            super.commence(request, response, authException);
        }
    }
}

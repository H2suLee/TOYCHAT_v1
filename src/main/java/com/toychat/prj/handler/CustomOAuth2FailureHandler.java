package com.toychat.prj.handler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.toychat.prj.common.util.Util;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomOAuth2FailureHandler implements AuthenticationFailureHandler{
	@Value("${spring.security.oauth2.callback-path}")
    private String callbackPath;
	
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException e) throws IOException, ServletException {
        String uri = Util.getBaseUrl(request);
        System.out.println("로그인 실패 핸들러 : " + uri);
        uri = "http://localhost:9091"; // 로컬용
    	
		String fullUrl = UriComponentsBuilder.fromUriString(uri + callbackPath)
				.queryParam("error",e.getLocalizedMessage() != null?e.getLocalizedMessage():"Server Error")
				.build()
				.encode()
				.toUriString();
    	
        response.sendRedirect(fullUrl);
    }
}

package com.toychat.prj.handler;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.toychat.prj.common.jwt.JwtUtil;
import com.toychat.prj.common.util.Util;
import com.toychat.prj.entity.CustomOAuth2User;
import com.toychat.prj.entity.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
	
	@Autowired
	private JwtUtil jwtUtil;
	
	@Value("${spring.security.oauth2.callback-path}")
    private String callbackPath;
	
	
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException, ServletException {
        String uri = Util.getBaseUrl(request);
        System.out.println("로그인 성공 핸들러 : " + uri);
        uri = "http://localhost:9091"; // 로컬용
        
        String fullUrl = "";
        CustomOAuth2User oauth2User = (CustomOAuth2User)authentication.getPrincipal();
        if (isUser(oauth2User)) {
        	System.out.println("로그인 성공");
        	User user = oauth2User.getUser();
        	String jwtToken = jwtUtil.generateToken(user.getId());
        	fullUrl = UriComponentsBuilder.fromUriString(uri + callbackPath)
                    .queryParam("id",user.getId())
                    .queryParam("nick",user.getNick())
                    .queryParam("jwt",jwtToken)
                    .build()
                    .encode()
                    .toUriString();
        	
            response.sendRedirect(fullUrl);
            
        } else {
        	System.out.println("로그인 실패");
        	fullUrl = UriComponentsBuilder.fromUriString(uri + callbackPath)
                    .queryParam("error","Not user")
                    .build()
                    .encode()
                    .toUriString();
        	
            response.sendRedirect(fullUrl);
        }
    }

    private boolean isUser(CustomOAuth2User oAuth2User) {
        return oAuth2User.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_USR"));
    }
}
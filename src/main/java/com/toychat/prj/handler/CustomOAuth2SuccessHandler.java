package com.toychat.prj.handler;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.toychat.prj.common.jwt.JwtUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
	
	@Autowired
	private JwtUtil jwtUtil;
	
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException, ServletException {
    	System.out.println("로그인 성공 핸들러");
        // 여기에 로그인 성공 후 처리할 내용을 작성하기!
        DefaultOAuth2User oauth2User = (DefaultOAuth2User)authentication.getPrincipal();
        if (isUser(oauth2User)) {
        	System.out.println("로그인 성공");
        	
        	Map<String, Object> attributes = oauth2User.getAttributes();
        	System.out.println("attributes2: {}" +  attributes.toString()); // {id=3651954457, connected_at=2024-08-05T07:26:46Z, properties={nickname=이희수}, kakao_account={profile_nickname_needs_agreement=false, profile={nickname=이희수, is_default_nickname=false}}}
        	System.out.println(attributes.get("id"));
        	String id = String.valueOf(attributes.get("id"));
        	Map<String, Object> properties =  (Map<String, Object>) attributes.get("properties");
        	String nick = (String) properties.get("nickname");
        	// jwtKey 부여
        	String jwtToken = jwtUtil.generateToken(id);
        	String redirectUrl = "http://localhost:9091"; // 프로퍼티로 관리하기
        	String redirectPath = "/login/oauth2/callback";
        	String fullUrl = UriComponentsBuilder.fromUriString(redirectUrl + redirectPath)
                    .queryParam("id",id)
                    .queryParam("nick",nick)
                    .queryParam("jwt",jwtToken)
                    .build()
                    .encode()
                    .toUriString();
        	
            response.sendRedirect(fullUrl);
            
        } else {
        	System.out.println("로그인 실패");
            //response.sendRedirect("/accerotuss-guest");
        }
    }

    private boolean isUser(DefaultOAuth2User oAuth2User) {
        return oAuth2User.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_USR"));
    }
}
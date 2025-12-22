package com.toychat.prj.handler;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
	
	private final JwtUtil jwtUtil;
	
	@Value("${spring.security.oauth2.callback-path}")
    private String callbackPath;
	
    @Value("${jwt.refreshExpiration}")
    private long jwtRefreshExpirationSeconds;
    
	private final RedisTemplate<String, Object> redisTemplate;
	
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException, ServletException {
        String uri = Util.getRedirectBaseUrl(request);
        //uri = "http://localhost:9091"; //9091에서 프론트 돌릴때만
        
        String fullUrl = "";
        CustomOAuth2User oauth2User = (CustomOAuth2User)authentication.getPrincipal();
        if (isUser(oauth2User)) {
        	User user = oauth2User.getUser();
    		// jwt refresh token 관리
    		String refreshToken = jwtUtil.generateRefeshToken(user.getId());
    		redisTemplate.opsForValue().set(
    			    "RT:" + user.getId(),
    			    refreshToken,
    			    jwtRefreshExpirationSeconds,   // TTL 값
    			    TimeUnit.SECONDS               // TTL 단위
    			);
        	
    		// https 쿠키 세팅
    		jwtUtil.addRefreshTokenCookie(response, refreshToken);
    		
        	String jwtToken = jwtUtil.generateAccessToken(user.getId());
        	fullUrl = UriComponentsBuilder.fromUriString(uri + callbackPath)
                    .queryParam("id",user.getId())
                    .queryParam("nick",user.getNick())
                    .queryParam("jwt",jwtToken)
                    .build()
                    .encode()
                    .toUriString();
        	
            response.sendRedirect(fullUrl);
            
        } else {
        	log.debug("로그인 실패");
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

package com.toychat.prj.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toychat.prj.common.jwt.JwtUtil;
import com.toychat.prj.entity.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/common")
public class CommonController {

	private final JwtUtil jwtUtil;
	
    @Value("${jwt.refreshExpiration}")
    private long jwtRefreshExpirationSeconds;
    
	private final RedisTemplate<String, Object> redisTemplate;
	
    @PostMapping("/refreshJwt")
    public User refreshJwt(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
    	User user = new User();
    	
    	 if (refreshToken != null) {
    		 String username = jwtUtil.extractUsername(refreshToken);
    		 String stored = (String) redisTemplate.opsForValue().get("RT:" + username);
    		 if (stored == null || !stored.equals(refreshToken)) {
    			 throw new RuntimeException("Invalid or expired refresh token");
    		 }

    		 // 새 Access Token 발급
    		 String newAccessToken = jwtUtil.generateAccessToken(username);
    		 user.setJwt(newAccessToken);
    	 }

        return user;
    }
    
    
    @GetMapping("/me")
    public ResponseEntity<?> checkAuth(Authentication authentication) {
    	
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);

        return ResponseEntity.ok(result);
    }
}

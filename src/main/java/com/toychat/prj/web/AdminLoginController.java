package com.toychat.prj.web;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toychat.prj.common.jwt.JwtUtil;
import com.toychat.prj.entity.User;
import com.toychat.prj.entity.UserDetailsImpl;
import com.toychat.prj.handler.CustomOAuth2SuccessHandler;
import com.toychat.prj.repository.UserRepository;
import com.toychat.prj.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
public class AdminLoginController {
	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserService userService;
	
    @Value("${jwt.refreshExpiration}")
    private long jwtRefreshExpirationSeconds;
    
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
    
	@PostMapping("/adminLogin")
    public User authenticate(@RequestBody User user, HttpServletResponse response) throws AuthenticationException {
		
		// 인증 처리
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(user.getId(), user.getPw()));
		
		// jwt refresh token 관리
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		String refreshToken = jwtUtil.generateRefeshToken(userDetails.getUsername());
		redisTemplate.opsForValue().set(
			    "RT:" + userDetails.getUsername(),
			    refreshToken,
			    jwtRefreshExpirationSeconds,   // TTL 값
			    TimeUnit.SECONDS               // TTL 단위
			);

		// https 쿠키 세팅
		jwtUtil.addRefreshTokenCookie(response, refreshToken);
		
		// 로그인정보 리턴
		String jwtToken = jwtUtil.generateAccessToken(userDetails.getUsername());
		user.setNick(userDetails.getUser().getNick());
		user.setJwt(jwtToken);
		log.debug("login complete");
        return user;
    }

    @PostMapping("/adminRegister")
    public String register(@RequestBody User user) {
    	log.debug("Registering..");
    	log.debug(user.toString());
        User existingUser = userService.findByUserId(user.getId());
        if (existingUser != null) {
            throw new RuntimeException("User already exists : " + existingUser.toString());
        }
        user.setPw(passwordEncoder.encode(user.getPw())); // 비밀번호 암호화
        userRepository.save(user);
        return "User registered successfully";
    }	

}

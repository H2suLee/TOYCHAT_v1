package com.toychat.prj.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.toychat.prj.common.jwt.JwtRequestFilter;
import com.toychat.prj.handler.CustomOAuth2FailureHandler;
import com.toychat.prj.handler.CustomOAuth2SuccessHandler;
import com.toychat.prj.service.CustomOAuth2UserService;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtRequestFilter jwtRequestFilter;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
    private final CustomOAuth2FailureHandler customOAuth2FailureHandler;
    
    public SecurityConfig(JwtRequestFilter jwtRequestFilter, CustomOAuth2SuccessHandler customOAuth2SuccessHandler, CustomOAuth2FailureHandler customOAuth2FailureHandler, CustomOAuth2UserService customOAuth2UserService) {
        this.jwtRequestFilter = jwtRequestFilter;
		this.customOAuth2SuccessHandler = customOAuth2SuccessHandler;
		this.customOAuth2FailureHandler = customOAuth2FailureHandler;
		this.customOAuth2UserService = customOAuth2UserService;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    	log.debug("securityFilterChain");
        http
            .csrf(AbstractHttpConfigurer::disable) // CSRF 비활성화
            .authorizeHttpRequests(auth -> auth
            	.requestMatchers(EndpointRequest.to("health", "info")).permitAll()
            	.requestMatchers("/", "/admin", "/admin/**", "/chat/**", "/login/oauth2/**", "/api/adminLogin", "/api/adminRegister", "/api/common/refreshJwt", "/api/common/me", "/api/fcm/**", "/index.html", "/js/**", "/css/**", "/img/**", "/favicon.ico", "/firebase-messaging-sw.js", "/ws/**").permitAll() // 정적 리소스 인증 필요 없는 경로
                .requestMatchers("/api/admin/**").hasRole("ADM")
                .requestMatchers("/api/users/**").hasRole("USR")
                .anyRequest().authenticated() // 다른 모든 요청은 인증 필요
           		//.anyRequest().permitAll()	
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 사용하지 않음
            )
            .formLogin(AbstractHttpConfigurer::disable)  // Form 로그인 비활성화
            .exceptionHandling()
            .authenticationEntryPoint((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)); // 시큐리티 인증 실패시 디폴트는 302 리턴했다가 /login 으로 리다이렉트하는데 401로 리턴하도록 하는 설정 
        
        http.oauth2Login(oauth2 -> oauth2
        		//.authorizationEndpoint(config -> config.baseUri("/oauth2/authorization"))
        		.successHandler(customOAuth2SuccessHandler)
        		.failureHandler(customOAuth2FailureHandler)
        		.userInfoEndpoint(config -> config.userService(customOAuth2UserService)) );
        
        //http.oauth2Login(Customizer.withDefaults());

        // JWT 필터를 UsernamePasswordAuthenticationFilter 전에 추가
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }    
}

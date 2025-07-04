package com.toychat.prj.config;

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
    	System.out.println("securityFilterChain");
        http
            .csrf(AbstractHttpConfigurer::disable) // CSRF 비활성화
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/login", "/api/admin/register", "/login/oauth2/**", "/ws/**").permitAll() // 인증 필요 없는 경로
                .requestMatchers("/admin/**").hasRole("ADM")
                .anyRequest().authenticated() // 다른 모든 요청은 인증 필요
           		//.anyRequest().permitAll()	
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 사용하지 않음
            );
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

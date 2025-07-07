package com.toychat.prj.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.toychat.prj.entity.CustomOAuth2User;
import com.toychat.prj.entity.OAuth2Type;
import com.toychat.prj.entity.User;
import com.toychat.prj.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService{
	@Autowired
	private UserRepository userRepository;
	
	private final OAuth2StrategyComposite oAuth2StrategyComposite;
	
	@Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		User user = null;
		OAuth2User oAuth2User = super.loadUser(userRequest);
		try {
			
			System.out.println("oAuth2User: {}" +  oAuth2User);
			System.out.println("provider info: " +  userRequest.getClientRegistration().getRegistrationId());
			
			user = oAuth2StrategyComposite
					.getOAuth2Strategy(getProvider(userRequest))
					.getUserInfo(oAuth2User);
			
			// 회원정보가 db에 없으면 저장
			if (userRepository.findById(user.getId()).orElse(null) == null) {
				userRepository.save(user);
			}
			return new CustomOAuth2User(user, oAuth2User.getAttributes());
			
		} catch (Exception e) {
			// 실패시 Handler로 넘기기
			e.printStackTrace();
			throw new OAuth2AuthenticationException(
		            new OAuth2Error("oauth2_user_load_error", e.getMessage(), null)
		        );
		}
		
	}

	private OAuth2Type getProvider(OAuth2UserRequest userRequest) {
		 return OAuth2Type.ofType(userRequest.getClientRegistration().getRegistrationId());
	}
	
}

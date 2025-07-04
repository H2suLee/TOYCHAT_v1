package com.toychat.prj.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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
		OAuth2User oAuth2User = super.loadUser(userRequest);
		System.out.println("oAuth2User: {}" +  oAuth2User);
		
        User user = oAuth2StrategyComposite
                .getOAuth2Strategy(getProvider(userRequest))
                .getUserInfo(oAuth2User);
		
		// 회원정보가 db에 없으면 저장
		if (userRepository.findById(user.getId()).orElse(null) == null) {
			userRepository.save(user);
        }
		
		return new CustomOAuth2User(user, oAuth2User.getAttributes());
	}

	private OAuth2Type getProvider(OAuth2UserRequest userRequest) {
		 return OAuth2Type.ofType(userRequest.getClientRegistration().getRegistrationId());
	}
	
}

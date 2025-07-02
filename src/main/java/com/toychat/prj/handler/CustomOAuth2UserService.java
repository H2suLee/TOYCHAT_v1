package com.toychat.prj.handler;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

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
	
	@Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oauth2User = super.loadUser(userRequest);
		System.out.println("oAuth2User: {}" +  oauth2User); // Name: [3651954457], Granted Authorities: [[OAUTH2_USER, SCOPE_profile_nickname]], User Attributes: [{id=3651954457, connected_at=2024-08-05T07:26:46Z, properties={nickname=이희수}, kakao_account={profile_nickname_needs_agreement=false, profile={nickname=이희수, is_default_nickname=false}}}]
		
		String providerCode = userRequest.getClientRegistration().getRegistrationId();
		System.out.println("providerCode:" +  providerCode); // kakao

		Map<String, Object> attributes = oauth2User.getAttributes();
		Map<String, Object> properties =  (Map<String, Object>) attributes.get("properties");
		System.out.println("attributes: {}" +  attributes.toString()); // {id=3651954457, connected_at=2024-08-05T07:26:46Z, properties={nickname=이희수}, kakao_account={profile_nickname_needs_agreement=false, profile={nickname=이희수, is_default_nickname=false}}}
		System.out.println("authtype: {}" +   userRequest.getClientRegistration().getRegistrationId()); // kakao
		System.out.println("nickname: {}" +   properties.get("nickname")); // kakao
		
		// 회원정보가 db에 없으면 저장
		String id = String.valueOf(attributes.get("id"));
		String role = "ROLE_USR";
		if (userRepository.findById(id).orElse(null) == null) {
			String nick = (String) properties.get("nickname");
			User user = new User();
			user.setId(id);
			user.setNick(nick);
			user.setRole(role);
			System.out.println("custom load user : " + user.toString());
			userRepository.save(user);
        }
		
		List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
		return new DefaultOAuth2User(authorities, oauth2User.getAttributes(), "id");
	}
}

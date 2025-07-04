package com.toychat.prj.service;

import java.util.Map;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import com.toychat.prj.entity.OAuth2Type;
import com.toychat.prj.entity.User;

@Component
public class KakaoOAuthStrategy implements OAuth2Strategy {

	@Override
	public OAuth2Type getOAuthProviderType() {
		return OAuth2Type.KAKAO;
	}

	@Override
	public User getUserInfo(OAuth2User oauth2User) {
		Map<String, Object> attributes = oauth2User.getAttributes();
		Map<String, Object> properties =  (Map<String, Object>) attributes.get("properties");
		String nick = (String) properties.get("nickname");
		String id = String.valueOf(attributes.get("id"));
		String role = "ROLE_USR";
		User user = new User();
		user.setId(id);
		user.setNick(nick);
		user.setRole(role);
		return user;
	}

}

package com.toychat.prj.service;

import java.util.Map;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import com.toychat.prj.entity.OAuth2Type;
import com.toychat.prj.entity.User;

@Component
public class NaverOAuth2Strategy implements OAuth2Strategy {

	@Override
	public OAuth2Type getOAuthProviderType() {
		return OAuth2Type.NAVER;
	}

	@Override
	public User getUserInfo(OAuth2User oauth2User) {
		Map<String, Object> attributes = oauth2User.getAttributes();
		Map<String, Object> response = (Map<String, Object>) attributes.get("response");
		String id = String.valueOf(response.get("id"));
		String nick = String.valueOf(response.get("name"));
		String role = "ROLE_USR";
		User user = new User();
		user.setId(id);
		user.setNick(nick);
		user.setRole(role);
		return user;
	}

}

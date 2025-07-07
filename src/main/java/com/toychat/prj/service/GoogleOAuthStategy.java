package com.toychat.prj.service;

import java.util.Map;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import com.toychat.prj.entity.OAuth2Type;
import com.toychat.prj.entity.User;

@Component
public class GoogleOAuthStategy implements OAuth2Strategy{

	@Override
	public OAuth2Type getOAuthProviderType() {
		return OAuth2Type.GOOGLE;
	}

	@Override
	public User getUserInfo(OAuth2User oauth2User) {
		Map<String, Object> attributes = oauth2User.getAttributes();
		String nick = (String) attributes.get("name");
		String id = (String) attributes.get("sub");
		String role = "ROLE_USR";
		User user = new User();
		user.setId(id);
		user.setNick(nick);
		user.setRole(role);
		return user;
	}

}

package com.toychat.prj.service;

import org.springframework.security.oauth2.core.user.OAuth2User;

import com.toychat.prj.entity.OAuth2Type;
import com.toychat.prj.entity.User;

public interface OAuth2Strategy {
    OAuth2Type getOAuthProviderType();

    User getUserInfo(OAuth2User user);

    default void isOauthIdExist(String oauthId) {
        if (null == oauthId) {
            throw new NullPointerException("oauthId does not exist");
        }
    }
}

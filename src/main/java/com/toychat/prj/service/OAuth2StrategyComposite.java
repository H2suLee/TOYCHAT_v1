package com.toychat.prj.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;

import com.toychat.prj.entity.OAuth2Type;

import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

@Component
public class OAuth2StrategyComposite {
	private final Map<OAuth2Type, OAuth2Strategy> oauthProviderMap;

	public OAuth2StrategyComposite(Set<OAuth2Strategy> clients) {
		this.oauthProviderMap = clients.stream()
                .collect(toMap(OAuth2Strategy::getOAuthProviderType, identity()));
	}

	public OAuth2Strategy getOAuth2Strategy(OAuth2Type provider) {
		return Optional.ofNullable(oauthProviderMap.get(provider))
                .orElseThrow(() -> new OAuth2AuthenticationException("not supported OAuth2 provider"));
	}
	
	
}

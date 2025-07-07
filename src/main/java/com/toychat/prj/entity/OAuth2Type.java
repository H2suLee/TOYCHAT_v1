package com.toychat.prj.entity;

import java.util.Arrays;

public enum OAuth2Type {
    KAKAO("kakao"), GITHUB("github"), NAVER("naver"), GOOGLE("google");

    private final String type;

    OAuth2Type(String type) {
        this.type = type;
    }

    public static OAuth2Type ofType(String type) {
        return Arrays.stream(values())
                .filter(oauthType -> oauthType.type.equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such OauthType"));
    }

    public String getType() {
        return type;
    }
}

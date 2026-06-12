package com.example.demo.security.oauth2;

import com.example.demo.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

@Getter
public class CustomOAuth2User implements OAuth2User {
    private final Integer userId;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;

    public CustomOAuth2User(Integer userId, String email, Collection<? extends GrantedAuthority> authorities, 
                          Map<String, Object> attributes, String nameAttributeKey) {
        this.userId = userId;
        this.email = email;
        this.authorities = authorities;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
    }

    public static CustomOAuth2User create(User user, Map<String, Object> attributes) {
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getAuthority())
        );

        return new CustomOAuth2User(
                user.getId().intValue(),
                user.getEmail(),
                authorities,
                attributes,
                "email"
        );
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return String.valueOf(attributes.get(nameAttributeKey));
    }
}

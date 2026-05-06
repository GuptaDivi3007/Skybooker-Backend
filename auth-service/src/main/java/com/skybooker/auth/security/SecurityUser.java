package com.skybooker.auth.security;

import com.skybooker.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

public class SecurityUser extends org.springframework.security.core.userdetails.User {

    private final String userId;
    private final String email;
    private final boolean active;

    public SecurityUser(User user) {
        super(
                user.getEmail(),
                user.getPasswordHash() == null ? "" : user.getPasswordHash(),
                user.isActive(),
                true,
                true,
                true,
                buildAuthorities(user)
        );
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.active = user.isActive();
    }

    private static Collection<? extends GrantedAuthority> buildAuthorities(User user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    public String getUserId() {
        return userId;
    }

    public String getEmailValue() {
        return email;
    }

    public boolean isActiveValue() {
        return active;
    }
}
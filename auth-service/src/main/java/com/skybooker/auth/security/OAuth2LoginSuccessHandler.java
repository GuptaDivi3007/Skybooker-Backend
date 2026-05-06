package com.skybooker.auth.security;

import com.skybooker.auth.entity.AuthProvider;
import com.skybooker.auth.entity.Role;
import com.skybooker.auth.entity.User;
import com.skybooker.auth.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        if (email == null || email.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not received from OAuth provider");
            return;
        }

        User user = userRepository.findByEmail(email.toLowerCase().trim()).orElseGet(() -> {
            User newUser = new User();
            newUser.setFullName(name == null || name.isBlank() ? "Google User" : name);
            newUser.setEmail(email);
            newUser.setPasswordHash(null);
            newUser.setPhone(null);
            newUser.setRole(Role.PASSENGER);
            newUser.setProvider(AuthProvider.GOOGLE);
            newUser.setActive(true);
            newUser.setPassportNumber(null);
            newUser.setNationality(null);
            return userRepository.save(newUser);
        });

        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getUserId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), user.getUserId());

        String finalRedirect = redirectUri
                + "?accessToken=" + accessToken
                + "&refreshToken=" + refreshToken
                + "&email=" + user.getEmail()
                + "&role=" + user.getRole().name();

        response.sendRedirect(finalRedirect);
    }
}
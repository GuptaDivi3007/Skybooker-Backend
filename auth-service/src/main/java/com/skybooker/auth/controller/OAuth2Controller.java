package com.skybooker.auth.controller;

import com.skybooker.auth.dto.MessageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OAuth2Controller {

    @GetMapping("/oauth2/info")
    public MessageResponse oauth2Info() {
        return new MessageResponse("Use /oauth2/authorization/google to login with Google");
    }
}
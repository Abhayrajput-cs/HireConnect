package com.hireconnect.web.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.hireconnect.web.dto.AuthResponse;
import com.hireconnect.web.dto.LoginForm;
import com.hireconnect.web.dto.PortalSession;
import com.hireconnect.web.dto.RegisterForm;
import com.hireconnect.web.support.GatewayClient;

@Service
public class AuthService {

    private final GatewayClient gatewayClient;

    public AuthService(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    public AuthResponse register(RegisterForm form) {
        return gatewayClient.post("/api/auth/register", null, Map.of(
            "email", form.getEmail(),
            "password", form.getPassword(),
            "role", form.getRole()
        ), AuthResponse.class);
    }

    public AuthResponse login(LoginForm form) {
        return gatewayClient.post("/api/auth/login", null, Map.of(
            "email", form.getEmail(),
            "password", form.getPassword()
        ), AuthResponse.class);
    }

    public PortalSession toPortalSession(AuthResponse authResponse) {
        return new PortalSession(
            authResponse.user().userId(),
            authResponse.user().email(),
            authResponse.user().role(),
            authResponse.accessToken(),
            authResponse.refreshToken()
        );
    }
}

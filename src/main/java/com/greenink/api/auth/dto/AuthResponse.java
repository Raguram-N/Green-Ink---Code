package com.greenink.api.auth.dto;

public record AuthResponse(String accessToken, long expiresIn, AuthUserResponse user) {}

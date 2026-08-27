package com.greenink.api.auth.dto;

import java.util.Set;

public record AuthUserResponse(String id, String identifier, Set<String> roles) {}

package com.greenink.api.profile.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PreferencesRequest(
        @NotNull @Pattern(regexp = "sm|md|lg") String textSize,
        @NotNull Boolean notificationsEnabled
) {}

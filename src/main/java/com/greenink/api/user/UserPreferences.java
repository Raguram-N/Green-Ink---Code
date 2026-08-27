package com.greenink.api.user;

public record UserPreferences(String textSize, boolean notificationsEnabled) {
    public static UserPreferences defaults() {
        return new UserPreferences("md", true);
    }
}

package com.greenink.api.notification;

import java.time.Instant;

public record NotificationItem(String id, String title, String message, Instant createdAt) {}

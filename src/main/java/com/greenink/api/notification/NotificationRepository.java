package com.greenink.api.notification;

import java.util.List;

public interface NotificationRepository {
    List<NotificationItem> findByUserId(String userId);
    int countByUserId(String userId);
    void deleteAllByUserId(String userId);
}

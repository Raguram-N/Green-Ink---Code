package com.greenink.api.infrastructure.memory;

import com.greenink.api.notification.NotificationItem;
import com.greenink.api.notification.NotificationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PlaceholderNotificationRepository implements NotificationRepository {
    @Override public List<NotificationItem> findByUserId(String userId) { return List.of(); }
    @Override public int countByUserId(String userId) { return 0; }
    @Override public void deleteAllByUserId(String userId) { }
}

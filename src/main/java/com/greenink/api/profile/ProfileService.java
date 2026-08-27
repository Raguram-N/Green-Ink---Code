package com.greenink.api.profile;

import com.greenink.api.auth.SessionRepository;
import com.greenink.api.common.NotFoundException;
import com.greenink.api.entitlement.SubscriptionRepository;
import com.greenink.api.notification.NotificationRepository;
import com.greenink.api.progress.ProgressRepository;
import com.greenink.api.progress.ProgressService;
import com.greenink.api.profile.dto.ProfileResponse;
import com.greenink.api.pyq.PyqAttemptRepository;
import com.greenink.api.search.SearchHistoryRepository;
import com.greenink.api.user.UserAccount;
import com.greenink.api.user.UserPreferences;
import com.greenink.api.user.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ProfileService {
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ProgressService progressService;
    private final ProgressRepository progressRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final SessionRepository sessionRepository;
    private final PyqAttemptRepository attemptRepository;
    private final NotificationRepository notificationRepository;

    public ProfileService(UserRepository userRepository, SubscriptionRepository subscriptionRepository,
                          ProgressService progressService, ProgressRepository progressRepository,
                          SearchHistoryRepository searchHistoryRepository, SessionRepository sessionRepository,
                          PyqAttemptRepository attemptRepository, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.progressService = progressService;
        this.progressRepository = progressRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.sessionRepository = sessionRepository;
        this.attemptRepository = attemptRepository;
        this.notificationRepository = notificationRepository;
    }

    public ProfileResponse get(String userId) {
        UserAccount user = requireUser(userId);
        var subscription = subscriptionRepository.findByUserId(userId).orElse(null);
        boolean premium = subscription != null && subscription.activeAt(Instant.now());
        return new ProfileResponse(user.id(), user.identifier(), user.roles(), premium,
                premium ? subscription.planCode() : null,
                premium ? subscription.expiresAt() : null,
                user.preferences(), notificationRepository.countByUserId(userId), progressService.getProgress(userId));
    }

    public UserPreferences updatePreferences(String userId, UserPreferences preferences) {
        UserAccount user = requireUser(userId);
        UserAccount updated = new UserAccount(user.id(), user.identifier(), user.roles(), user.createdAt(), preferences);
        userRepository.save(updated);
        return updated.preferences();
    }

    public void deleteAccount(String userId) {
        requireUser(userId);
        sessionRepository.deleteAllByUserId(userId);
        progressRepository.deleteUser(userId);
        searchHistoryRepository.deleteAll(userId);
        subscriptionRepository.deleteByUserId(userId);
        attemptRepository.deleteAllByUserId(userId);
        notificationRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }

    private UserAccount requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User account not found."));
    }
}

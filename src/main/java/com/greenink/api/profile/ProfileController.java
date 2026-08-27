package com.greenink.api.profile;

import com.greenink.api.profile.dto.PreferencesRequest;
import com.greenink.api.profile.dto.ProfileResponse;
import com.greenink.api.security.SecurityUtil;
import com.greenink.api.user.UserPreferences;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) { this.profileService = profileService; }

    @GetMapping
    public ProfileResponse me() { return profileService.get(SecurityUtil.requireUserId()); }

    @GetMapping("/preferences")
    public UserPreferences preferences() { return profileService.get(SecurityUtil.requireUserId()).preferences(); }

    @PutMapping("/preferences")
    public UserPreferences updatePreferences(@Valid @RequestBody PreferencesRequest request) {
        return profileService.updatePreferences(SecurityUtil.requireUserId(),
                new UserPreferences(request.textSize(), request.notificationsEnabled()));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount() {
        profileService.deleteAccount(SecurityUtil.requireUserId());
        return ResponseEntity.noContent().build();
    }
}

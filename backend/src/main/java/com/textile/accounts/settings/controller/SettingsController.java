package com.textile.accounts.settings.controller;

import com.textile.accounts.settings.dto.SettingsDtos.BusinessProfileResponse;
import com.textile.accounts.settings.dto.SettingsDtos.BusinessProfileUpdateRequest;
import com.textile.accounts.settings.dto.SettingsDtos.LockDateUpdateRequest;
import com.textile.accounts.settings.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/profile")
    public ResponseEntity<BusinessProfileResponse> getProfile() {
        return ResponseEntity.ok(settingsService.getProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<BusinessProfileResponse> updateProfile(
            @Valid @RequestBody BusinessProfileUpdateRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(settingsService.updateProfile(request, actor));
    }

    @PutMapping("/lock-date")
    public ResponseEntity<BusinessProfileResponse> updateLockDate(
            @Valid @RequestBody LockDateUpdateRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(settingsService.updateLockDate(request, actor));
    }
}

package com.palmonas.crm.module.order.controller;

import com.palmonas.crm.common.dto.ApiResponse;
import com.palmonas.crm.config.FeatureFlagService;
import com.palmonas.crm.module.channel.ChannelSyncService;
import com.palmonas.crm.module.channel.adapter.ChannelAdapter;
import com.palmonas.crm.module.notification.model.DeadLetterEntry;
import com.palmonas.crm.module.notification.service.DeadLetterService;
import com.palmonas.crm.module.order.model.FeatureFlag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin", description = "Admin-only endpoints for system management")
public class AdminController {

    private final FeatureFlagService featureFlagService;
    private final DeadLetterService deadLetterService;
    private final ChannelSyncService channelSyncService;
    private final List<ChannelAdapter> channelAdapters;

    @GetMapping("/feature-flags")
    @Operation(summary = "Get all feature flags")
    public ResponseEntity<ApiResponse<List<FeatureFlag>>> getFeatureFlags() {
        return ResponseEntity.ok(ApiResponse.ok(featureFlagService.getAll()));
    }

    @PutMapping("/feature-flags/{key}")
    @Operation(summary = "Toggle a feature flag")
    public ResponseEntity<ApiResponse<FeatureFlag>> toggleFlag(
            @PathVariable String key,
            @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", false);
        FeatureFlag flag = featureFlagService.toggle(key, enabled);
        return ResponseEntity.ok(ApiResponse.ok(flag, "Feature flag updated"));
    }

    @GetMapping("/dead-letters")
    @Operation(summary = "Get pending dead letter entries")
    public ResponseEntity<ApiResponse<Page<DeadLetterEntry>>> getDeadLetters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(deadLetterService.getPending(page, size)));
    }

    @PostMapping("/dead-letters/{id}/resolve")
    @Operation(summary = "Mark a dead letter entry as resolved")
    public ResponseEntity<ApiResponse<Void>> resolveDeadLetter(@PathVariable UUID id) {
        deadLetterService.markResolved(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Marked as resolved"));
    }

    @PostMapping("/sync-channels")
    @Operation(summary = "Manually trigger channel sync")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> syncChannels() {
        Map<String, Integer> results = new java.util.LinkedHashMap<>();
        for (ChannelAdapter adapter : channelAdapters) {
            int count = channelSyncService.syncChannel(adapter);
            results.put(adapter.getChannelName(), count);
        }
        return ResponseEntity.ok(ApiResponse.ok(results, "Channel sync completed"));
    }
}

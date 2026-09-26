package com.toollix.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionDto(
        UUID id,
        String deviceName,
        String ipAddress,
        String location,
        Instant createdAt,
        Instant lastUsedAt,
        boolean revoked) {
}

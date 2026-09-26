package com.toollix.auth.dto;

import java.util.UUID;

public record SessionDto(UUID id, String deviceName, boolean revoked) {}

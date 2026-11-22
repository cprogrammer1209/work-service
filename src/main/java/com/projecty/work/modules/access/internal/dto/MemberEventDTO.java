package com.projecty.work.modules.access.internal.dto;

import java.util.List;
import java.util.UUID;

/**
 * The part of a Platform member event Work needs. {@code profileKey} and {@code permissions} are null on events
 * written before Platform started sending them.
 */
public record MemberEventDTO(UUID eventId, String type, UUID portalId, UUID userId, String profileKey,
		List<String> permissions) {
}

package com.projecty.work.modules.access.api.dto;

import java.util.Set;
import java.util.UUID;

/**
 * @param profileKey the member's profile in that portal, e.g. ADMIN; the Admin profile bypasses project roles
 * @param permissions permission names granted by that profile
 * @param active false once the member was deactivated
 */
public record MemberAccessResponseDTO(UUID portalId, UUID userId, String profileKey, Set<String> permissions,
		boolean active) {
}

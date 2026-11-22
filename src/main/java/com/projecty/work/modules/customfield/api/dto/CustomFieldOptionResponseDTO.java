package com.projecty.work.modules.customfield.api.dto;

import java.util.UUID;

/**
 * @param active false once deactivated: hidden from new choices, still shown on old records as "inactive"
 */
public record CustomFieldOptionResponseDTO(UUID id, String label, String color, int sortOrder, boolean defaultOption,
		boolean active) {
}

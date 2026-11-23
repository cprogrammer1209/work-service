package com.projecty.work.modules.customfield.api.dto;

import java.util.UUID;

/**
 * A status of a TASK layout. {@code statusType} is OPEN or CLOSED; exactly one status of a layout is the default.
 */
public record LayoutStatusResponseDTO(UUID id, String name, String statusType, String color, int sortOrder,
		boolean defaultStatus) {
}

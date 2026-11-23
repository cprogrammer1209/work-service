package com.projecty.work.modules.customfield.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * A layout ready to draw a form: sections in order, each with its active fields and their effective rules.
 * {@code statuses} lists the statuses of a TASK layout in order (empty for other modules).
 */
public record LayoutResponseDTO(UUID id, String entityType, String name, String description, boolean defaultLayout,
		boolean active, List<LayoutSectionResponseDTO> sections, List<LayoutStatusResponseDTO> statuses) {
}

package com.projecty.work.modules.customfield.api.dto;

import java.util.List;
import java.util.UUID;

/** A layout ready to draw a form: sections in order, each with its active fields and their effective rules. */
public record LayoutResponseDTO(UUID id, String entityType, String name, String description, boolean defaultLayout,
		boolean active, List<LayoutSectionResponseDTO> sections) {
}

package com.projecty.work.modules.customfield.internal.dto;

import java.util.UUID;

/** One row of the layouts list. {@code usage} is how many records use the layout. */
public record LayoutSummaryResponseDTO(UUID id, String entityType, String name, String description,
		boolean defaultLayout, boolean active, int sectionCount, int fieldCount, long usage) {
}

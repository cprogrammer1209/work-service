package com.projecty.work.modules.customfield.internal.dto;

import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.customfield.api.dto.CustomFieldResponseDTO;

/**
 * What the layout builder needs: the layout as stored, plus every non-deleted field of the module (active and
 * inactive) so unplaced ones can be offered.
 */
public record LayoutEditorResponseDTO(UUID id, String entityType, String name, String description,
		boolean defaultLayout, boolean active, long usage, List<LayoutSectionEditorResponseDTO> sections,
		List<CustomFieldResponseDTO> fields) {
}

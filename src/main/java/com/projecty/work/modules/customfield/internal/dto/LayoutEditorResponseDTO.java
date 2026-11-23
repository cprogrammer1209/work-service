package com.projecty.work.modules.customfield.internal.dto;

import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.customfield.api.dto.CustomFieldResponseDTO;
import com.projecty.work.modules.customfield.api.dto.LayoutStatusResponseDTO;

/**
 * What the layout builder needs: the layout as stored, plus every non-deleted field of the module (active and
 * inactive) so unplaced ones can be offered. {@code statuses} lists the statuses of a TASK layout (empty for other
 * modules).
 */
public record LayoutEditorResponseDTO(UUID id, String entityType, String name, String description,
		boolean defaultLayout, boolean active, long usage, List<LayoutSectionEditorResponseDTO> sections,
		List<CustomFieldResponseDTO> fields, List<LayoutStatusResponseDTO> statuses) {
}

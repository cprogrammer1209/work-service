package com.projecty.work.modules.customfield.internal.dto;

import java.util.List;
import java.util.UUID;

public record LayoutSectionEditorResponseDTO(UUID id, String name, boolean collapsible,
		List<LayoutPlacementResponseDTO> fields) {
}

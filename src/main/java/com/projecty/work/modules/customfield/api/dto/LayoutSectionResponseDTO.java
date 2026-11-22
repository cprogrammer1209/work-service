package com.projecty.work.modules.customfield.api.dto;

import java.util.List;
import java.util.UUID;

/** A titled group of fields in a layout, in display order. Only active fields are listed. */
public record LayoutSectionResponseDTO(UUID id, String name, boolean collapsible, List<LayoutFieldResponseDTO> fields) {
}

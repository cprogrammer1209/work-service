package com.projecty.work.modules.customfield.internal.dto;

import java.util.List;

import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A section as the layout builder saves it; the order of the lists is the display order. */
public record LayoutSectionUpdateDTO(@NotNull @Size(max = CustomFieldConstants.SECTION_NAME_MAX_LENGTH) String name,
		boolean collapsible, @Valid List<LayoutFieldUpdateDTO> fields) {
}

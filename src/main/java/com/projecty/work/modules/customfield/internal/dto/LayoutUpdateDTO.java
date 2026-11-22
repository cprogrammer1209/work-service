package com.projecty.work.modules.customfield.internal.dto;

import java.util.List;

import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** The whole layout as the builder saves it: it replaces the name, description, sections and placed fields. */
public record LayoutUpdateDTO(@NotBlank @Size(max = CustomFieldConstants.LAYOUT_NAME_MAX_LENGTH) String name,
		@Size(max = CustomFieldConstants.LAYOUT_DESCRIPTION_MAX_LENGTH) String description,
		@NotEmpty @Valid List<LayoutSectionUpdateDTO> sections) {
}

package com.projecty.work.modules.customfield.internal.dto;

import java.util.UUID;

import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param entityType the module the layout is for, e.g. PROJECT
 * @param cloneFromId copy the sections and placed fields of this layout; null starts with one empty section
 */
public record LayoutCreateDTO(@NotBlank String entityType,
		@NotBlank @Size(max = CustomFieldConstants.LAYOUT_NAME_MAX_LENGTH) String name,
		@Size(max = CustomFieldConstants.LAYOUT_DESCRIPTION_MAX_LENGTH) String description, UUID cloneFromId) {
}

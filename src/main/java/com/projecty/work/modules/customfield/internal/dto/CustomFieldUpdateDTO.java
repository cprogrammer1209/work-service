package com.projecty.work.modules.customfield.internal.dto;

import java.util.Map;

import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The edit form sends every editable property (a null default or config clears it). The key, the type and
 * "unique" cannot change after creation.
 */
public record CustomFieldUpdateDTO(@NotBlank @Size(max = CustomFieldConstants.LABEL_MAX_LENGTH) String label,
		@Size(max = CustomFieldConstants.DESCRIPTION_MAX_LENGTH) String description, boolean mandatory,
		boolean sensitive, Object defaultValue, Map<String, Object> config) {
}

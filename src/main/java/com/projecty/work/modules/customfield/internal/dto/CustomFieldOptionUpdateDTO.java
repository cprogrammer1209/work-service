package com.projecty.work.modules.customfield.internal.dto;

import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The edit form sends every property. {@code active = false} hides the choice from new answers; old records keep it.
 */
public record CustomFieldOptionUpdateDTO(@NotBlank @Size(max = CustomFieldConstants.LABEL_MAX_LENGTH) String label,
		@Size(max = CustomFieldConstants.COLOR_MAX_LENGTH) String color, boolean defaultOption, boolean active) {
}

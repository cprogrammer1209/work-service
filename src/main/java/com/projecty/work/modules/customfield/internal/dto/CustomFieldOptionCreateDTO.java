package com.projecty.work.modules.customfield.internal.dto;

import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomFieldOptionCreateDTO(@NotBlank @Size(max = CustomFieldConstants.LABEL_MAX_LENGTH) String label,
		@Size(max = CustomFieldConstants.COLOR_MAX_LENGTH) String color, boolean defaultOption) {
}

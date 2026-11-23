package com.projecty.work.modules.customfield.internal.dto;

import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;
import com.projecty.work.modules.customfield.internal.entity.LayoutStatusType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param statusType OPEN or CLOSED
 * @param color a CSS color for the status chip, e.g. #2563eb; optional
 */
public record LayoutStatusCreateDTO(
		@NotBlank(message = CustomFieldConstants.MESSAGE_STATUS_NAME_REQUIRED) @Size(max = CustomFieldConstants.STATUS_NAME_MAX_LENGTH) String name,
		@NotNull(message = CustomFieldConstants.MESSAGE_STATUS_TYPE_REQUIRED) LayoutStatusType statusType,
		@Size(max = CustomFieldConstants.STATUS_COLOR_MAX_LENGTH) String color) {
}

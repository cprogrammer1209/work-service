package com.projecty.work.modules.customfield.internal.dto;

import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;
import com.projecty.work.modules.customfield.internal.entity.LayoutStatusType;

import jakarta.validation.constraints.Size;

/**
 * A partial update: a property left out (null) stays as it is; an empty color clears the color. {@code defaultStatus}
 * can only be set to true (it moves the default to this status); to change the default, set it on the new one.
 */
public record LayoutStatusUpdateDTO(@Size(max = CustomFieldConstants.STATUS_NAME_MAX_LENGTH) String name,
		LayoutStatusType statusType, @Size(max = CustomFieldConstants.STATUS_COLOR_MAX_LENGTH) String color,
		Boolean defaultStatus) {
}

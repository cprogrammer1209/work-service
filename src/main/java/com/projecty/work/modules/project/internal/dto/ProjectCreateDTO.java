package com.projecty.work.modules.project.internal.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.projecty.work.modules.project.internal.constants.ProjectConstants;
import com.projecty.work.modules.project.internal.entity.ProjectVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param ownerUserId defaults to the caller; must be an active member of the portal
 */
public record ProjectCreateDTO(@NotBlank @Size(max = ProjectConstants.NAME_MAX_LENGTH) String name,
		@NotBlank @Pattern(regexp = ProjectConstants.KEY_PATTERN, message = "Use 2 to 10 capital letters or numbers.") String key,
		UUID ownerUserId, LocalDate startDate, LocalDate endDate, @NotNull ProjectVisibility visibility,
		@Size(max = ProjectConstants.DESCRIPTION_MAX_LENGTH) String description) {
}

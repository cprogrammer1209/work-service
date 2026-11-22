package com.projecty.work.modules.project.internal.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.projecty.work.modules.project.internal.constants.ProjectConstants;
import com.projecty.work.modules.project.internal.entity.ProjectVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The editable fields. The key is not among them: it never changes (PRJ-BR-01). A null owner keeps the owner.
 */
public record ProjectUpdateDTO(@NotBlank @Size(max = ProjectConstants.NAME_MAX_LENGTH) String name, UUID ownerUserId,
		LocalDate startDate, LocalDate endDate, @NotNull ProjectVisibility visibility,
		@Size(max = ProjectConstants.DESCRIPTION_MAX_LENGTH) String description) {
}

package com.projecty.work.modules.project.internal.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.projecty.work.modules.project.internal.entity.ProjectVisibility;

/**
 * A project as the caller may see it. {@code canEdit} and {@code canDelete} tell the UI which actions to show;
 * the server still checks. {@code progress} is 0 until tasks exist. {@code deletedAt} is set only in the trash.
 */
public record ProjectResponseDTO(UUID id, String key, String name, String description, String status,
		LocalDate startDate, LocalDate endDate, ProjectVisibility visibility, UUID ownerUserId, int memberCount,
		int progress, boolean member, boolean canEdit, boolean canDelete, Instant createdAt, Instant deletedAt,
		UUID deletedByUserId) {
}

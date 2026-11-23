package com.projecty.work.modules.task.internal.dto;

import java.time.Instant;
import java.util.UUID;

public record TaskCommentResponseDTO(UUID id, UUID taskId, UUID authorUserId, String authorName, String body,
		boolean edited, Instant createdAt, Instant updatedAt, boolean canEdit, boolean canDelete) {
}

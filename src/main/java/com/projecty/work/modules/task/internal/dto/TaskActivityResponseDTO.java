package com.projecty.work.modules.task.internal.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * One entry of a task's history. {@code action} is TASK_CREATED, TASK_UPDATED (with {@code field}, {@code oldValue},
 * {@code newValue}), TASK_STATUS_CHANGED (status names in the values), TASK_TRASHED, TASK_RESTORED or one of the
 * TASK_COMMENT_* actions.
 */
public record TaskActivityResponseDTO(UUID id, String action, String field, String oldValue, String newValue,
		String detail, UUID actorUserId, String actorName, Instant createdAt) {
}

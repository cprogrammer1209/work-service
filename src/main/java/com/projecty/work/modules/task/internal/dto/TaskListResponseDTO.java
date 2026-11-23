package com.projecty.work.modules.task.internal.dto;

import java.time.Instant;
import java.util.UUID;

import com.projecty.work.modules.task.internal.entity.TaskListVisibility;

/**
 * A task list with its counts of tasks (trashed ones excluded) and of closed tasks among them.
 */
public record TaskListResponseDTO(UUID id, UUID projectId, String name, TaskListVisibility visibility,
		int sortOrder, boolean archived, int taskCount, int completedCount, Instant createdAt) {
}

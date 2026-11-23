package com.projecty.work.modules.task.internal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.projecty.work.modules.task.internal.entity.TaskPriority;

/**
 * A task as the caller may see it. {@code projectKey} and {@code projectName} name the project. {@code key} is the display id (WEB-T42). {@code statusType} is OPEN or CLOSED.
 * {@code subtaskCount} and {@code completedSubtaskCount} count the direct subtasks (trashed ones excluded).
 * {@code customValues} holds the answers of the task layout's placed fields; {@code canEdit} and {@code canDelete}
 * tell the UI which actions to show (the server still checks). {@code deletedAt} is set only in the trash.
 */
public record TaskResponseDTO(UUID id, UUID projectId, String projectKey, String projectName, String key, int number, UUID listId,
		String listName, UUID parentId, int depth, String title, String description, UUID statusId,
		String statusName, String statusType, String statusColor, TaskPriority priority, LocalDate startDate,
		LocalDate dueDate, BigDecimal workHours, int completion, List<UUID> ownerIds, List<UUID> followerIds,
		List<String> tags, Map<String, Object> customValues, UUID layoutId, int subtaskCount,
		int completedSubtaskCount, int commentCount, Instant closedAt, Instant createdAt, UUID createdBy,
		Instant updatedAt, Instant deletedAt, boolean canEdit, boolean canDelete) {
}

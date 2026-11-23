package com.projecty.work.modules.task.internal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.projecty.work.modules.task.internal.entity.TaskPriority;

/**
 * A new task; only the title is needed (quick add). The limits are checked by the service, which answers with every
 * problem at once under {@code details}.
 * @param listId the task list; the project's first active list ("General" is created when it has none) when absent;
 * ignored for a subtask, which takes its parent's list
 * @param parentId makes the task a subtask of that task (at most 5 levels)
 * @param statusId a status of the project's task layout; its default status when absent
 * @param priority NONE when absent
 * @param ownerIds active members of the portal; the owners and the creator follow the task
 * @param tags free text, at most 20 of 30 characters, case-insensitive duplicates dropped
 * @param customValues answers to the portal's TASK custom fields, keyed by api_name
 */
public record TaskCreateDTO(String title, String description, UUID listId, UUID parentId, UUID statusId,
		TaskPriority priority, LocalDate startDate, LocalDate dueDate, BigDecimal workHours, Integer completion,
		List<UUID> ownerIds, List<String> tags, Map<String, Object> customValues) {
}

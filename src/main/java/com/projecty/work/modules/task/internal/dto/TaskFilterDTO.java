package com.projecty.work.modules.task.internal.dto;

import java.util.UUID;

import com.projecty.work.modules.task.internal.entity.TaskPriority;

/**
 * The filters of a project's task list; every one is optional and they combine with AND.
 * @param statusId tasks in that status
 * @param statusType OPEN or CLOSED
 * @param q matches the title, the display id (WEB-T42) or a tag, ignoring case
 * @param parentId only the direct subtasks of that task
 * @param trashed the trash instead of the live tasks (needs TASKS_DELETE)
 */
public record TaskFilterDTO(UUID listId, UUID statusId, String statusType, UUID ownerId, TaskPriority priority,
		String q, UUID parentId, boolean trashed) {
}

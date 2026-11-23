package com.projecty.work.modules.task.internal.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.projecty.work.modules.task.internal.entity.TaskPriority;

/**
 * The filters of the portal-wide task list; every one is optional and they combine with AND.
 * @param scope mine (the caller is an owner) or all (default)
 * @param statusType OPEN or CLOSED
 * @param q matches the title, the display id (WEB-T42) or a tag, ignoring case
 * @param dueBefore due on or before that day (tasks without a due date never match)
 * @param dueAfter due on or after that day (tasks without a due date never match)
 * @param limit page size, 1..500, default 200
 * @param cursor the nextCursor of the previous page
 */
public record TaskAcrossFilterDTO(String scope, UUID projectId, String statusType, UUID ownerId,
		TaskPriority priority, String q, LocalDate dueBefore, LocalDate dueAfter, Integer limit, String cursor) {
}

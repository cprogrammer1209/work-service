package com.projecty.work.modules.task.internal.dto;

import java.util.List;

/**
 * One page of the portal-wide task list. {@code nextCursor} is null on the last page; {@code total} counts every
 * task that matches the filters and that the caller may see.
 */
public record TaskPageResponseDTO(List<TaskResponseDTO> items, String nextCursor, int total) {
}

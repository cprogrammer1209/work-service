package com.projecty.work.modules.task.internal.dto;

import com.projecty.work.modules.task.internal.entity.TaskListVisibility;

/** @param visibility INTERNAL when absent */
public record TaskListCreateDTO(String name, TaskListVisibility visibility) {
}

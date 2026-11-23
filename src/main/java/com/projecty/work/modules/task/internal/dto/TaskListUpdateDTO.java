package com.projecty.work.modules.task.internal.dto;

import com.projecty.work.modules.task.internal.entity.TaskListVisibility;

/** A partial update: a property left out (null) stays as it is. */
public record TaskListUpdateDTO(String name, TaskListVisibility visibility) {
}

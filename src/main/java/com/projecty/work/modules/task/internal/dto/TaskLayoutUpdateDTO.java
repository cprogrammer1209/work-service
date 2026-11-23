package com.projecty.work.modules.task.internal.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** @param layoutId an active TASK layout of the task's portal */
public record TaskLayoutUpdateDTO(@NotNull UUID layoutId) {
}

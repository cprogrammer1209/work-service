package com.projecty.work.modules.task.internal.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** @param statusId a status of the task's layout */
public record TaskStatusUpdateDTO(@NotNull UUID statusId) {
}

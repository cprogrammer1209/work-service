package com.projecty.work.modules.task.internal.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** @param ids task list ids in the new order; lists left out follow in their current order */
public record TaskOrderDTO(@NotNull List<UUID> ids) {
}

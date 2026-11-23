package com.projecty.work.modules.customfield.internal.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** @param ids the layout's status ids in the new order; each status once */
public record LayoutStatusOrderDTO(@NotNull List<UUID> ids) {
}

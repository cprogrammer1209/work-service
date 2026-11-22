package com.projecty.work.modules.customfield.internal.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * @param fieldIds the fields of the module in their new order
 */
public record CustomFieldReorderDTO(@NotBlank String entityType, @NotEmpty List<UUID> fieldIds) {
}

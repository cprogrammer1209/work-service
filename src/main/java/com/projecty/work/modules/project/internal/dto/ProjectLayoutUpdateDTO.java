package com.projecty.work.modules.project.internal.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** @param layoutId an active layout of the project's portal */
public record ProjectLayoutUpdateDTO(@NotNull UUID layoutId) {
}

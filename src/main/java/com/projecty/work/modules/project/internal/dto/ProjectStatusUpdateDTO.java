package com.projecty.work.modules.project.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectStatusUpdateDTO(@NotBlank String status) {
}

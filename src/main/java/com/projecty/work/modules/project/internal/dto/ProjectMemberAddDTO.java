package com.projecty.work.modules.project.internal.dto;

import java.util.UUID;

import com.projecty.work.modules.project.internal.entity.ProjectRole;

import jakarta.validation.constraints.NotNull;

/**
 * @param role defaults to MEMBER
 */
public record ProjectMemberAddDTO(@NotNull UUID userId, ProjectRole role) {
}

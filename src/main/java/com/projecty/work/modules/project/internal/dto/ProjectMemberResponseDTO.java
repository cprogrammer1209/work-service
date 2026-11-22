package com.projecty.work.modules.project.internal.dto;

import java.util.UUID;

import com.projecty.work.modules.project.internal.entity.ProjectRole;

public record ProjectMemberResponseDTO(UUID userId, ProjectRole role, boolean owner) {
}

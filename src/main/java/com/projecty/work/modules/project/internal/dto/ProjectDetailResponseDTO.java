package com.projecty.work.modules.project.internal.dto;

import java.util.List;

public record ProjectDetailResponseDTO(ProjectResponseDTO project, List<ProjectMemberResponseDTO> members,
		List<ProjectActivityResponseDTO> activity) {
}

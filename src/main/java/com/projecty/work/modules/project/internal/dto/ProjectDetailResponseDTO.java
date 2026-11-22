package com.projecty.work.modules.project.internal.dto;

import java.util.List;
import java.util.Map;

import com.projecty.work.modules.customfield.api.dto.LayoutResponseDTO;

/**
 * A project with its members and recent activity, and the layout it uses. {@code project.customValues} holds the
 * answers of the fields in that layout; {@code hiddenValues} holds the answers of active fields the layout does not
 * show (kept, and shown again if the project moves to a layout that has them).
 */
public record ProjectDetailResponseDTO(ProjectResponseDTO project, List<ProjectMemberResponseDTO> members,
		List<ProjectActivityResponseDTO> activity, LayoutResponseDTO layout, Map<String, Object> hiddenValues) {
}

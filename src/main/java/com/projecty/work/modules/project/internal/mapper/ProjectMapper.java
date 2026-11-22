package com.projecty.work.modules.project.internal.mapper;

import java.util.Map;

import com.projecty.work.modules.project.internal.constants.ProjectConstants;
import com.projecty.work.modules.project.internal.dto.ProjectActivityResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectMemberResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectStatusResponseDTO;
import com.projecty.work.modules.project.internal.entity.Project;
import com.projecty.work.modules.project.internal.entity.ProjectActivity;
import com.projecty.work.modules.project.internal.entity.ProjectMember;

import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

	/**
	 * Progress comes from closed tasks (Step 6), so it is 0 until they exist.
	 */
	public ProjectResponseDTO toResponse(Project project, int memberCount, boolean member, boolean canEdit,
			boolean canDelete, Map<String, Object> customValues) {
		return new ProjectResponseDTO(project.getId(), project.getProjectKey(), project.getName(),
				project.getDescription(), project.getStatus(), project.getStartDate(), project.getEndDate(),
				project.getVisibility(), project.getOwnerUserId(), memberCount, 0, member, canEdit, canDelete,
				project.getCreatedAt(), project.getDeletedAt(), project.getDeletedByUserId(), customValues);
	}

	public ProjectMemberResponseDTO toMember(ProjectMember member, Project project) {
		return new ProjectMemberResponseDTO(member.getUserId(), member.getRole(),
				member.getUserId().equals(project.getOwnerUserId()));
	}

	public ProjectActivityResponseDTO toActivity(ProjectActivity activity) {
		return new ProjectActivityResponseDTO(activity.getAction(), activity.getActorUserId(), activity.getActorName(),
				activity.getTargetUserId(), activity.getDetail(), activity.getCreatedAt());
	}

	public ProjectStatusResponseDTO toStatus(ProjectConstants.StatusDefinition status) {
		return new ProjectStatusResponseDTO(status.name(), status.type());
	}

}

package com.projecty.work.modules.project.internal.service;

import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.project.internal.dto.KeyAvailabilityResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectCreateDTO;
import com.projecty.work.modules.project.internal.dto.ProjectDetailResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectIdResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectMemberAddDTO;
import com.projecty.work.modules.project.internal.dto.ProjectMemberResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectStatusResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectUpdateDTO;

/**
 * Projects of the request's portal (X-Portal-Id). Every method takes the caller; what the caller may see or change
 * depends on their profile and their role in the project.
 */
public interface ProjectService {

	List<ProjectResponseDTO> list(UUID actorId);

	List<ProjectResponseDTO> listTrash(UUID actorId);

	ProjectDetailResponseDTO get(UUID actorId, UUID projectId);

	ProjectIdResponseDTO create(UUID actorId, String actorName, ProjectCreateDTO request);

	ProjectResponseDTO update(UUID actorId, String actorName, UUID projectId, ProjectUpdateDTO request);

	ProjectResponseDTO changeStatus(UUID actorId, String actorName, UUID projectId, String status);

	void trash(UUID actorId, String actorName, UUID projectId);

	ProjectResponseDTO restore(UUID actorId, String actorName, UUID projectId);

	List<ProjectMemberResponseDTO> addMember(UUID actorId, String actorName, UUID projectId, ProjectMemberAddDTO request);

	List<ProjectMemberResponseDTO> removeMember(UUID actorId, String actorName, UUID projectId, UUID userId);

	KeyAvailabilityResponseDTO checkKey(String key);

	List<ProjectStatusResponseDTO> statuses();

}

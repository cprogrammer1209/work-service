package com.projecty.work.modules.project.api;

import java.util.Optional;
import java.util.UUID;

import com.projecty.work.modules.project.api.dto.ProjectSummaryDTO;

/**
 * What other modules ask of projects. The project module never calls back into them.
 */
public interface ProjectLookUpService {

	/**
	 * The project, if it is in the portal, not in the trash, and the user may see it under the project rules (Admins
	 * see all; others see projects they are in and public ones, Clients never public ones). Empty otherwise, so the
	 * caller answers "not found" for another portal's project and for one the user cannot see alike.
	 * @throws com.projecty.work.common.exception.AppException 403 when the user is not an active member of the portal
	 */
	Optional<ProjectSummaryDTO> findVisibleProject(UUID portalId, UUID projectId, UUID userId);

}

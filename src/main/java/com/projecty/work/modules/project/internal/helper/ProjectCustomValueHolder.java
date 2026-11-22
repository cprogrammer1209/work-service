package com.projecty.work.modules.project.internal.helper;

import java.util.UUID;

import com.projecty.work.modules.customfield.api.CustomValueHolder;
import com.projecty.work.modules.project.internal.constants.ProjectConstants;
import com.projecty.work.modules.project.internal.repository.ProjectRepository;

import org.springframework.stereotype.Component;

/**
 * Lets the customfield module count and clear the answers stored on projects, without it touching the project table.
 */
@Component
public class ProjectCustomValueHolder implements CustomValueHolder {

	private final ProjectRepository projectRepository;

	public ProjectCustomValueHolder(ProjectRepository projectRepository) {
		this.projectRepository = projectRepository;
	}

	@Override
	public String entityType() {
		return ProjectConstants.CUSTOM_FIELD_ENTITY;
	}

	@Override
	public long countUsing(UUID portalId, String apiName) {
		return this.projectRepository.countWithCustomKey(portalId, apiName);
	}

	@Override
	public void removeKey(UUID portalId, String apiName) {
		this.projectRepository.removeCustomKey(portalId, apiName);
	}

}

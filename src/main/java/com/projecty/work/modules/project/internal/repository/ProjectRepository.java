package com.projecty.work.modules.project.internal.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projecty.work.modules.project.internal.entity.Project;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

	List<Project> findByPortalIdAndDeletedAtIsNull(UUID portalId);

	List<Project> findByPortalIdAndDeletedAtIsNotNull(UUID portalId);

	Optional<Project> findByIdAndPortalId(UUID id, UUID portalId);

	/**
	 * Includes trashed projects: a key stays taken until the project is gone for good.
	 */
	boolean existsByPortalIdAndProjectKey(UUID portalId, String projectKey);

}

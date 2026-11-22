package com.projecty.work.modules.project.internal.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projecty.work.modules.project.internal.entity.Project;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

	List<Project> findByPortalIdAndDeletedAtIsNull(UUID portalId);

	List<Project> findByPortalIdAndDeletedAtIsNotNull(UUID portalId);

	Optional<Project> findByIdAndPortalId(UUID id, UUID portalId);

	/** Same lookup, but holds a row lock until the transaction ends, so two edits of one project queue up. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Project p where p.id = :id and p.portalId = :portalId")
	Optional<Project> lockByIdAndPortalId(@Param("id") UUID id, @Param("portalId") UUID portalId);

	/** Projects in the trash count too: restoring one must find its layout still counted. */
	long countByPortalIdAndLayoutId(UUID portalId, UUID layoutId);

	@Query(value = "SELECT count(*) FROM project WHERE portal_id = :portalId AND custom_values ->> CAST(:key AS text) IS NOT NULL",
			nativeQuery = true)
	long countWithCustomKey(@Param("portalId") UUID portalId, @Param("key") String key);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "UPDATE project SET custom_values = custom_values - CAST(:key AS text) WHERE portal_id = :portalId AND custom_values ->> CAST(:key AS text) IS NOT NULL",
			nativeQuery = true)
	int removeCustomKey(@Param("portalId") UUID portalId, @Param("key") String key);

	/**
	 * Includes trashed projects: a key stays taken until the project is gone for good.
	 */
	boolean existsByPortalIdAndProjectKey(UUID portalId, String projectKey);

}

package com.projecty.work.modules.task.internal.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projecty.work.modules.task.internal.entity.Task;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID> {

	List<Task> findByPortalIdAndProjectIdAndDeletedAtIsNull(UUID portalId, UUID projectId);

	List<Task> findByPortalIdAndProjectIdAndDeletedAtIsNotNull(UUID portalId, UUID projectId);

	/** The projects of the portal that have live tasks (the cross-project list looks only at these). */
	@Query("select distinct t.projectId from Task t where t.portalId = :portalId and t.deletedAt is null")
	List<UUID> findProjectIdsWithLiveTasks(@Param("portalId") UUID portalId);

	Optional<Task> findByIdAndPortalId(UUID id, UUID portalId);

	/** Holds a row lock until the transaction ends, so two edits of one task queue up. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Task t where t.id = :id and t.portalId = :portalId")
	Optional<Task> lockByIdAndPortalId(@Param("id") UUID id, @Param("portalId") UUID portalId);

	/**
	 * Hands out the next task number of a project. The upsert locks the sequence row until the transaction ends, so
	 * two creations queue up and a number is never given twice; a rolled-back creation gives its number back.
	 */
	@Query(value = "INSERT INTO task_sequence (project_id, portal_id, last_number) VALUES (:projectId, :portalId, 1) "
			+ "ON CONFLICT (project_id) DO UPDATE SET last_number = task_sequence.last_number + 1 RETURNING last_number",
			nativeQuery = true)
	int nextNumber(@Param("projectId") UUID projectId, @Param("portalId") UUID portalId);

	/** Trashed tasks count too: restoring one must find its layout and status still counted. */
	long countByPortalIdAndLayoutId(UUID portalId, UUID layoutId);

	long countByPortalIdAndStatusId(UUID portalId, UUID statusId);

	@Query(value = "SELECT count(*) FROM task WHERE portal_id = :portalId AND custom_values ->> CAST(:key AS text) IS NOT NULL",
			nativeQuery = true)
	long countWithCustomKey(@Param("portalId") UUID portalId, @Param("key") String key);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "UPDATE task SET custom_values = custom_values - CAST(:key AS text) WHERE portal_id = :portalId AND custom_values ->> CAST(:key AS text) IS NOT NULL",
			nativeQuery = true)
	int removeCustomKey(@Param("portalId") UUID portalId, @Param("key") String key);

}

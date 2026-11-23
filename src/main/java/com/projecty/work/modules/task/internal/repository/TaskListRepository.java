package com.projecty.work.modules.task.internal.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projecty.work.modules.task.internal.entity.TaskList;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskListRepository extends JpaRepository<TaskList, UUID> {

	List<TaskList> findByPortalIdAndProjectIdOrderBySortOrderAscCreatedAtAscIdAsc(UUID portalId, UUID projectId);

	Optional<TaskList> findByIdAndPortalId(UUID id, UUID portalId);

	/**
	 * Creates the project's default list unless a list of that name exists (any case). Safe when two requests race:
	 * the second one inserts nothing instead of failing.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "INSERT INTO task_list (id, portal_id, project_id, name, visibility, sort_order, archived, created_at, created_by, updated_at) "
			+ "SELECT gen_random_uuid(), :portalId, :projectId, :name, 'INTERNAL', "
			+ "COALESCE((SELECT max(sort_order) + 1 FROM task_list WHERE project_id = :projectId), 0), FALSE, :now, :userId, :now "
			+ "ON CONFLICT (project_id, lower(name)) DO NOTHING", nativeQuery = true)
	int insertDefaultIfMissing(@Param("portalId") UUID portalId, @Param("projectId") UUID projectId,
			@Param("name") String name, @Param("userId") UUID userId, @Param("now") Instant now);

}

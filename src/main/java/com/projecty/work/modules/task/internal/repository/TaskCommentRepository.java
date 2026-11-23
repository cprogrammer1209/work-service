package com.projecty.work.modules.task.internal.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projecty.work.modules.task.internal.entity.TaskComment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {

	List<TaskComment> findByPortalIdAndTaskIdOrderByCreatedAtAscIdAsc(UUID portalId, UUID taskId);

	Optional<TaskComment> findByIdAndPortalId(UUID id, UUID portalId);

	/** Comments per task for every task of a project. */
	@Query("select c.taskId as taskId, count(c) as total from TaskComment c where c.portalId = :portalId and c.taskId in "
			+ "(select t.id from Task t where t.portalId = :portalId and t.projectId = :projectId) group by c.taskId")
	List<CommentCount> countByTask(@Param("portalId") UUID portalId, @Param("projectId") UUID projectId);

	interface CommentCount {

		UUID getTaskId();

		long getTotal();

	}

}

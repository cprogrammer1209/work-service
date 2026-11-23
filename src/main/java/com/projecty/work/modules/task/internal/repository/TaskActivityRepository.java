package com.projecty.work.modules.task.internal.repository;

import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.task.internal.entity.TaskActivity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskActivityRepository extends JpaRepository<TaskActivity, UUID> {

	List<TaskActivity> findByPortalIdAndTaskIdOrderByCreatedAtDescIdDesc(UUID portalId, UUID taskId, Pageable pageable);

}

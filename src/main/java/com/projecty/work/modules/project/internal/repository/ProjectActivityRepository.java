package com.projecty.work.modules.project.internal.repository;

import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.project.internal.entity.ProjectActivity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectActivityRepository extends JpaRepository<ProjectActivity, UUID> {

	List<ProjectActivity> findByPortalIdAndProjectIdOrderByCreatedAtDesc(UUID portalId, UUID projectId, Pageable page);

}

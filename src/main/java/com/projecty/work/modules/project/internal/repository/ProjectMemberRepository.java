package com.projecty.work.modules.project.internal.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projecty.work.modules.project.internal.entity.ProjectMember;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMember.Key> {

	List<ProjectMember> findByPortalIdAndProjectId(UUID portalId, UUID projectId);

	List<ProjectMember> findByPortalIdAndUserId(UUID portalId, UUID userId);

	Optional<ProjectMember> findByPortalIdAndProjectIdAndUserId(UUID portalId, UUID projectId, UUID userId);

	@Query("select m.projectId as projectId, count(m) as total from ProjectMember m where m.portalId = :portalId group by m.projectId")
	List<MemberCount> countByProject(@Param("portalId") UUID portalId);

	interface MemberCount {

		UUID getProjectId();

		long getTotal();

	}

}

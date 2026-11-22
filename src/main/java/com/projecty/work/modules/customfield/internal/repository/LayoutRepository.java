package com.projecty.work.modules.customfield.internal.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projecty.work.modules.customfield.internal.entity.Layout;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LayoutRepository extends JpaRepository<Layout, UUID> {

	List<Layout> findByPortalIdAndEntityTypeOrderByCreatedAtAscIdAsc(UUID portalId, String entityType);

	Optional<Layout> findByIdAndPortalId(UUID id, UUID portalId);

	Optional<Layout> findByPortalIdAndEntityTypeAndDefaultLayoutTrue(UUID portalId, String entityType);

}

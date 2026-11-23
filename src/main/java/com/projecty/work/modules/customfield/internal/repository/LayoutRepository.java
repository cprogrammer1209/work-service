package com.projecty.work.modules.customfield.internal.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projecty.work.modules.customfield.internal.entity.Layout;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LayoutRepository extends JpaRepository<Layout, UUID> {

	List<Layout> findByPortalIdAndEntityTypeOrderByCreatedAtAscIdAsc(UUID portalId, String entityType);

	Optional<Layout> findByIdAndPortalId(UUID id, UUID portalId);

	Optional<Layout> findByPortalIdAndEntityTypeAndDefaultLayoutTrue(UUID portalId, String entityType);

	/** Holds a row lock until the transaction ends, so two status edits of one layout queue up. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select l from Layout l where l.id = :id and l.portalId = :portalId")
	Optional<Layout> lockByIdAndPortalId(@Param("id") UUID id, @Param("portalId") UUID portalId);

}

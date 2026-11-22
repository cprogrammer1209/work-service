package com.projecty.work.modules.customfield.internal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projecty.work.modules.customfield.internal.entity.CustomField;
import com.projecty.work.modules.customfield.internal.entity.FieldStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomFieldRepository extends JpaRepository<CustomField, UUID> {

	List<CustomField> findByPortalIdAndEntityTypeAndStatusInOrderBySortOrderAscCreatedAtAsc(UUID portalId,
			String entityType, Collection<FieldStatus> statuses);

	Optional<CustomField> findByIdAndPortalId(UUID id, UUID portalId);

	/** Counts deleted fields too: a key is never reused. */
	boolean existsByPortalIdAndEntityTypeAndApiName(UUID portalId, String entityType, String apiName);

	long countByPortalIdAndEntityTypeAndStatusNot(UUID portalId, String entityType, FieldStatus status);

	Optional<CustomField> findFirstByPortalIdAndEntityTypeOrderBySortOrderDesc(UUID portalId, String entityType);

}

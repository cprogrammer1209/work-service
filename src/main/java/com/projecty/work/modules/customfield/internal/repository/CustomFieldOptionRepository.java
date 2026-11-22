package com.projecty.work.modules.customfield.internal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projecty.work.modules.customfield.internal.entity.CustomFieldOption;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomFieldOptionRepository extends JpaRepository<CustomFieldOption, UUID> {

	List<CustomFieldOption> findByPortalIdAndFieldIdInOrderBySortOrderAsc(UUID portalId, Collection<UUID> fieldIds);

	List<CustomFieldOption> findByPortalIdAndFieldIdOrderBySortOrderAsc(UUID portalId, UUID fieldId);

	Optional<CustomFieldOption> findByIdAndPortalIdAndFieldId(UUID id, UUID portalId, UUID fieldId);

}

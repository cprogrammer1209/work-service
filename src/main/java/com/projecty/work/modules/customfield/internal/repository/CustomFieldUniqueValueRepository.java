package com.projecty.work.modules.customfield.internal.repository;

import java.util.UUID;

import com.projecty.work.modules.customfield.internal.entity.CustomFieldUniqueValue;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomFieldUniqueValueRepository
		extends JpaRepository<CustomFieldUniqueValue, CustomFieldUniqueValue.Key> {

	boolean existsByPortalIdAndFieldIdAndValueKey(UUID portalId, UUID fieldId, String valueKey);

	void deleteByPortalIdAndFieldIdAndEntityId(UUID portalId, UUID fieldId, UUID entityId);

	void deleteByPortalIdAndFieldId(UUID portalId, UUID fieldId);

}

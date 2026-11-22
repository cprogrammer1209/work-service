package com.projecty.work.modules.customfield.internal.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.customfield.internal.entity.LayoutField;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LayoutFieldRepository extends JpaRepository<LayoutField, UUID> {

	List<LayoutField> findByPortalIdAndLayoutIdInOrderBySortOrderAsc(UUID portalId, Collection<UUID> layoutIds);

	void deleteByPortalIdAndLayoutId(UUID portalId, UUID layoutId);

	/** A deleted field leaves every layout. */
	void deleteByPortalIdAndFieldId(UUID portalId, UUID fieldId);

	long countByPortalIdAndLayoutId(UUID portalId, UUID layoutId);

}

package com.projecty.work.modules.customfield.internal.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.customfield.internal.entity.LayoutSection;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LayoutSectionRepository extends JpaRepository<LayoutSection, UUID> {

	List<LayoutSection> findByPortalIdAndLayoutIdInOrderBySortOrderAsc(UUID portalId, Collection<UUID> layoutIds);

	void deleteByPortalIdAndLayoutId(UUID portalId, UUID layoutId);

}

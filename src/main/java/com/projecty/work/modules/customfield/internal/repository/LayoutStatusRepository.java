package com.projecty.work.modules.customfield.internal.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projecty.work.modules.customfield.internal.entity.LayoutStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LayoutStatusRepository extends JpaRepository<LayoutStatus, UUID> {

	List<LayoutStatus> findByPortalIdAndLayoutIdOrderBySortOrderAscIdAsc(UUID portalId, UUID layoutId);

	Optional<LayoutStatus> findByIdAndPortalIdAndLayoutId(UUID id, UUID portalId, UUID layoutId);

}

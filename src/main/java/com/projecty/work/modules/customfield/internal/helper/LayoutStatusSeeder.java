package com.projecty.work.modules.customfield.internal.helper;

import java.util.UUID;

import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;
import com.projecty.work.modules.customfield.internal.entity.LayoutStatus;
import com.projecty.work.modules.customfield.internal.repository.LayoutStatusRepository;

import org.springframework.stereotype.Component;

/**
 * Gives a new TASK layout its statuses: the standard three, or a copy of another layout's.
 */
@Component
public class LayoutStatusSeeder {

	private final LayoutStatusRepository statusRepository;

	public LayoutStatusSeeder(LayoutStatusRepository statusRepository) {
		this.statusRepository = statusRepository;
	}

	/** Open (default), In Progress and Closed. */
	public void seedDefaults(UUID portalId, UUID layoutId) {
		int order = 0;
		for (CustomFieldConstants.DefaultStatus status : CustomFieldConstants.DEFAULT_STATUSES) {
			this.statusRepository.save(new LayoutStatus(portalId, layoutId, status.name(), status.type(),
					status.color(), order, order == 0));
			order++;
		}
	}

	public void copy(UUID portalId, UUID sourceLayoutId, UUID targetLayoutId) {
		for (LayoutStatus status : this.statusRepository.findByPortalIdAndLayoutIdOrderBySortOrderAscIdAsc(portalId,
				sourceLayoutId)) {
			this.statusRepository.save(new LayoutStatus(portalId, targetLayoutId, status.getName(),
					status.getStatusType(), status.getColor(), status.getSortOrder(), status.isDefaultStatus()));
		}
	}

}

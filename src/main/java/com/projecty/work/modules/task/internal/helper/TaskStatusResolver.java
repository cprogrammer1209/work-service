package com.projecty.work.modules.task.internal.helper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.projecty.work.modules.customfield.api.LayoutLookUpService;
import com.projecty.work.modules.customfield.api.dto.LayoutStatusResponseDTO;

import org.springframework.stereotype.Component;

/** Looks up the statuses of the layouts a set of tasks use, through the customfield module's port. */
@Component
public class TaskStatusResolver {

	private final LayoutLookUpService layoutLookUpService;

	public TaskStatusResolver(LayoutLookUpService layoutLookUpService) {
		this.layoutLookUpService = layoutLookUpService;
	}

	/** The statuses of the given layouts, by status id. */
	public Map<UUID, LayoutStatusResponseDTO> byId(UUID portalId, Collection<UUID> layoutIds) {
		Map<UUID, LayoutStatusResponseDTO> statuses = new HashMap<>();
		for (UUID layoutId : new java.util.HashSet<>(layoutIds)) {
			this.layoutLookUpService.findStatuses(portalId, layoutId).forEach((status) -> statuses.put(status.id(), status));
		}
		return statuses;
	}

}

package com.projecty.work.modules.customfield.internal.service;

import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.customfield.api.dto.LayoutStatusResponseDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutStatusCreateDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutStatusOrderDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutStatusUpdateDTO;

/**
 * The statuses of a TASK layout of the request's portal (X-Portal-Id). Every method returns the layout's full
 * status list afterwards. A layout always keeps at least one Open and one Closed status, exactly one default (an Open
 * one), and a status that records use cannot be deleted.
 */
public interface LayoutStatusService {

	List<LayoutStatusResponseDTO> list(UUID layoutId);

	List<LayoutStatusResponseDTO> create(UUID actorId, String actorName, UUID layoutId, LayoutStatusCreateDTO request);

	List<LayoutStatusResponseDTO> update(UUID actorId, String actorName, UUID layoutId, UUID statusId,
			LayoutStatusUpdateDTO request);

	List<LayoutStatusResponseDTO> delete(UUID actorId, String actorName, UUID layoutId, UUID statusId);

	List<LayoutStatusResponseDTO> reorder(UUID actorId, String actorName, UUID layoutId, LayoutStatusOrderDTO request);

}

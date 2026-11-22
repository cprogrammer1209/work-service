package com.projecty.work.modules.customfield.internal.service;

import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.customfield.api.CustomFieldLookUpService;
import com.projecty.work.modules.customfield.api.CustomFieldWriteService;
import com.projecty.work.modules.customfield.api.dto.CustomFieldResponseDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldCreateDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldOptionCreateDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldOptionUpdateDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldReorderDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldUpdateDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldUsageResponseDTO;

/**
 * The custom fields of the request's portal (X-Portal-Id): what other modules use through the ports, and what the
 * admin screens manage.
 */
public interface CustomFieldService extends CustomFieldLookUpService, CustomFieldWriteService {

	/** Active fields of a module (what a form shows). */
	List<CustomFieldResponseDTO> listActive(String entityType);

	/** Active and inactive fields of a module (what the admin screen shows). */
	List<CustomFieldResponseDTO> listAll(String entityType);

	CustomFieldResponseDTO create(UUID actorId, String actorName, CustomFieldCreateDTO request);

	CustomFieldResponseDTO update(UUID actorId, String actorName, UUID fieldId, CustomFieldUpdateDTO request);

	CustomFieldResponseDTO deactivate(UUID actorId, String actorName, UUID fieldId);

	CustomFieldResponseDTO activate(UUID actorId, String actorName, UUID fieldId);

	CustomFieldUsageResponseDTO usage(UUID fieldId);

	/** Removes the field's answers from every record and retires its key for good. */
	void delete(UUID actorId, String actorName, UUID fieldId);

	List<CustomFieldResponseDTO> reorder(CustomFieldReorderDTO request);

	CustomFieldResponseDTO addOption(UUID actorId, String actorName, UUID fieldId, CustomFieldOptionCreateDTO request);

	CustomFieldResponseDTO updateOption(UUID actorId, String actorName, UUID fieldId, UUID optionId,
			CustomFieldOptionUpdateDTO request);

}

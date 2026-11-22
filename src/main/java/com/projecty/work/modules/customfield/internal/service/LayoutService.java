package com.projecty.work.modules.customfield.internal.service;

import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.customfield.api.LayoutLookUpService;
import com.projecty.work.modules.customfield.api.dto.LayoutResponseDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutCreateDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutEditorResponseDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutSummaryResponseDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutUpdateDTO;
import com.projecty.work.modules.customfield.internal.entity.CustomField;
import com.projecty.work.modules.customfield.internal.helper.FieldRule;

/**
 * The layouts of the request's portal (X-Portal-Id): what other modules read through the lookup port, what the admin
 * screens manage, and what the field service and the validator need inside this module.
 */
public interface LayoutService extends LayoutLookUpService {

	/** The active layouts of a module ready to draw a form (default first). */
	List<LayoutResponseDTO> listActive(String entityType);

	/** Every layout of a module with its counts, for the layouts list. */
	List<LayoutSummaryResponseDTO> listSummaries(String entityType);

	/** One layout as stored, plus every field that could be placed in it. */
	LayoutEditorResponseDTO editor(UUID layoutId);

	LayoutSummaryResponseDTO create(UUID actorId, String actorName, LayoutCreateDTO request);

	/** Replaces the name, description, sections and placed fields in one go. */
	LayoutEditorResponseDTO update(UUID actorId, String actorName, UUID layoutId, LayoutUpdateDTO request);

	LayoutSummaryResponseDTO makeDefault(UUID actorId, String actorName, UUID layoutId);

	LayoutSummaryResponseDTO activate(UUID actorId, String actorName, UUID layoutId);

	LayoutSummaryResponseDTO deactivate(UUID actorId, String actorName, UUID layoutId);

	/** A field was just created: it goes at the end of the default layout's last section. */
	void placeNewField(UUID portalId, CustomField field);

	/** A field was deleted: it leaves every layout. */
	void removePlacements(UUID portalId, UUID fieldId);

	/** The active fields of a layout with the layout's rules, for the validator. */
	List<FieldRule> rulesFor(UUID portalId, String entityType, UUID layoutId);

}

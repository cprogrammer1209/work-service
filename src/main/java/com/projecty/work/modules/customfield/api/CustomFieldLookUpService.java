package com.projecty.work.modules.customfield.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.projecty.work.modules.customfield.api.dto.CustomFieldResponseDTO;

/**
 * What other modules ask of the custom fields of a module (an {@code entityType} such as PROJECT).
 */
public interface CustomFieldLookUpService {

	/** The active fields of a module in form order, with their choices (inactive choices included, flagged). */
	List<CustomFieldResponseDTO> findFields(UUID portalId, String entityType);

	/**
	 * The single gate for every save of custom values (form, inline edit, import, API), checked against the layout the
	 * record uses. {@code input} holds the keys to set; a null or empty value clears a key. {@code existing} is what the
	 * record stores now. Rejects keys that are inactive or not in the layout, applies the layout's defaults on create,
	 * checks type, range, regex, choices, people, mandatory (the layout's override beats the field's flag; on create:
	 * every mandatory field; on update: the keys sent) and read-only fields, and returns the full cleaned map to store.
	 * Answers outside the layout stay as they are. Throws VALIDATION_FAILED with {key: message} details.
	 */
	Map<String, Object> validate(UUID portalId, String entityType, UUID layoutId, Map<String, Object> input,
			Map<String, Object> existing, boolean creating);

	/** The keys whose answers differ between two maps (numbers compared by value, not by Java type). */
	Set<String> changedKeys(Map<String, Object> before, Map<String, Object> after);

}

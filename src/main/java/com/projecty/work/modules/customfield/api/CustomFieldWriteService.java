package com.projecty.work.modules.customfield.api;

import java.util.Map;
import java.util.UUID;

/**
 * Writes other modules need from the custom fields.
 */
public interface CustomFieldWriteService {

	/**
	 * Keeps the unique-value reservations of one record in step with its answers: frees the old value, reserves the
	 * new one, and refuses (VALIDATION_FAILED) a value another record already has. Call it in the same transaction
	 * as the save of the record.
	 */
	void reserveUniqueValues(UUID portalId, String entityType, UUID entityId, Map<String, Object> before,
			Map<String, Object> after);

}

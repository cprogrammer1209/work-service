package com.projecty.work.modules.customfield.api.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A custom field as a form needs it. {@code apiName} is the key of its answer in a record's custom values.
 * {@code fieldType} is one of TEXT, LONG_TEXT, NUMBER, DECIMAL, DATE, CHECKBOX, PICKLIST, MULTI_PICKLIST, USER;
 * {@code status} is ACTIVE or INACTIVE; {@code config} holds the per-type rules (min, max, decimals, maxLength,
 * regex, maxSelections).
 */
public record CustomFieldResponseDTO(UUID id, String entityType, String apiName, String label, String description,
		String fieldType, boolean mandatory, boolean unique, boolean sensitive, Object defaultValue,
		Map<String, Object> config, String status, int sortOrder, List<CustomFieldOptionResponseDTO> options) {
}

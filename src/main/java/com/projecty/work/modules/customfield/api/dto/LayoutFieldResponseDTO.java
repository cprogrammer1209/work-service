package com.projecty.work.modules.customfield.api.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A custom field as a layout shows it: the field's own definition with the layout's rules already applied.
 * {@code mandatory}, {@code defaultValue} and {@code wide} are the effective values (a layout override beats the field);
 * {@code fieldMandatory} is what the field itself says, so a screen can show "same as the field".
 */
public record LayoutFieldResponseDTO(UUID fieldId, String apiName, String label, String description, String fieldType,
		boolean mandatory, boolean fieldMandatory, boolean readOnly, boolean wide, boolean unique, boolean sensitive,
		Object defaultValue, Map<String, Object> config, List<CustomFieldOptionResponseDTO> options) {
}

package com.projecty.work.modules.customfield.internal.dto;

import java.util.List;
import java.util.Map;

import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;
import com.projecty.work.modules.customfield.internal.entity.FieldType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param entityType the module the field is for, e.g. PROJECT
 * @param unique no two records may share an answer (text, number, decimal and date fields only)
 * @param defaultValue applied to new records; picklists, multi-selects and user fields use default choices instead
 * @param config per-type rules: min, max, decimals, maxLength, regex, maxSelections
 * @param options the choices of a picklist or multi-select
 */
public record CustomFieldCreateDTO(@NotBlank String entityType,
		@NotBlank @Size(max = CustomFieldConstants.LABEL_MAX_LENGTH) String label,
		@Size(max = CustomFieldConstants.DESCRIPTION_MAX_LENGTH) String description, @NotNull FieldType fieldType,
		boolean mandatory, boolean unique, boolean sensitive, Object defaultValue, Map<String, Object> config,
		@Valid List<CustomFieldOptionCreateDTO> options) {
}

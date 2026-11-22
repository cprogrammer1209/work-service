package com.projecty.work.modules.customfield.internal.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A field placed in a section.
 *
 * @param apiName the key of the field
 * @param mandatory true or false overrides the field; null means "as the field says"
 * @param readOnly people can see the value but not change it in this layout
 * @param defaultValue this layout's own default (an option id for a picklist); null means the field's default
 * @param wide true takes the whole row, false one column; null decides by the field type
 */
public record LayoutFieldUpdateDTO(@NotBlank String apiName, Boolean mandatory, boolean readOnly, Object defaultValue,
		Boolean wide) {
}

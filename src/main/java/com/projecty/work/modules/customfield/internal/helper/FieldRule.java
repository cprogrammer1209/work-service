package com.projecty.work.modules.customfield.internal.helper;

import com.projecty.work.modules.customfield.internal.entity.CustomField;

/**
 * A field together with the rules of the layout it is checked in: the effective required flag, whether it is read-only
 * there, and the layout's own default answer as JSON text (null = use the field's default).
 */
public record FieldRule(CustomField field, boolean mandatory, boolean readOnly, String defaultOverride) {
}

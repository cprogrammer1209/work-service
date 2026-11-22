package com.projecty.work.modules.customfield.internal.entity;

/**
 * ACTIVE fields show on forms. INACTIVE ones are hidden but keep their answers. DELETED ones have had their answers
 * removed; the row stays so the key is never reused.
 */
public enum FieldStatus {

	ACTIVE, INACTIVE, DELETED

}

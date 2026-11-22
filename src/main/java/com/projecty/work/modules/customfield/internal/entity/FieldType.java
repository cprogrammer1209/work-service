package com.projecty.work.modules.customfield.internal.entity;

/**
 * The kinds of custom field. A field's type cannot change after it is created (CUS-BR-02).
 */
public enum FieldType {

	TEXT, LONG_TEXT, NUMBER, DECIMAL, DATE, CHECKBOX, PICKLIST, MULTI_PICKLIST, USER;

	/** Picklists take their answers from the field's options. */
	public boolean hasOptions() {
		return this == PICKLIST || this == MULTI_PICKLIST;
	}

}

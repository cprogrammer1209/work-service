package com.projecty.work.modules.customfield.internal.constants;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import com.projecty.work.modules.customfield.internal.entity.FieldType;

/**
 * Constants of the customfield module: limits, config keys, error codes and messages.
 */
public final class CustomFieldConstants {

	/** Only these types can be marked unique: a whole-record duplicate check makes no sense for the others. */
	public static final Set<FieldType> UNIQUE_TYPES = EnumSet.of(FieldType.TEXT, FieldType.NUMBER, FieldType.DECIMAL,
			FieldType.DATE);

	/** The modules that can have custom fields today. Tasks and others are added here when they exist. */
	public static final String ENTITY_PROJECT = "PROJECT";

	public static final Set<String> ENTITY_TYPES = Set.of(ENTITY_PROJECT);

	/** The key of an answer is this prefix plus the label in snake_case, e.g. cf_customer_tier. */
	public static final String KEY_PREFIX = "cf_";

	public static final int API_NAME_MAX_LENGTH = 60;

	public static final String KEY_FALLBACK = "field";

	public static final int LABEL_MAX_LENGTH = 100;

	public static final int DESCRIPTION_MAX_LENGTH = 500;

	public static final int COLOR_MAX_LENGTH = 20;

	/** CUS-BR-13: fields per module on the default plan (deleted fields do not count). */
	public static final int FIELD_LIMIT = 25;

	public static final int OPTION_LIMIT = 200;

	public static final int TEXT_MAX_LENGTH = 255;

	public static final int LONG_TEXT_MAX_LENGTH = 10000;

	public static final int REGEX_MAX_LENGTH = 200;

	public static final int DECIMALS_DEFAULT = 2;

	public static final int DECIMALS_MAX = 6;

	public static final int SELECTIONS_DEFAULT = 20;

	public static final int SELECTIONS_MAX = 100;

	/** Numbers stay small enough to survive the JSON round trip exactly (12 digits, at most 6 decimals). */
	public static final BigDecimal NUMBER_MAX_ABS = new BigDecimal("999999999999");

	public static final String CONFIG_MIN = "min";

	public static final String CONFIG_MAX = "max";

	public static final String CONFIG_DECIMALS = "decimals";

	public static final String CONFIG_MAX_LENGTH = "maxLength";

	public static final String CONFIG_REGEX = "regex";

	public static final String CONFIG_MAX_SELECTIONS = "maxSelections";

	public static final String ERROR_FIELD_NOT_FOUND = "CUSTOM_FIELD_NOT_FOUND";

	public static final String ERROR_OPTION_NOT_FOUND = "CUSTOM_FIELD_OPTION_NOT_FOUND";

	public static final String ERROR_ENTITY_UNSUPPORTED = "CUSTOM_FIELD_MODULE_UNSUPPORTED";

	public static final String ERROR_FIELD_LIMIT = "CUSTOM_FIELD_LIMIT";

	public static final String ERROR_FIELD_INVALID = "CUSTOM_FIELD_INVALID";

	public static final String ERROR_OPTION_LIMIT = "CUSTOM_FIELD_OPTION_LIMIT";

	public static final String MESSAGE_FIELD_NOT_FOUND = "That custom field does not exist.";

	public static final String MESSAGE_OPTION_NOT_FOUND = "That choice does not exist.";

	public static final String MESSAGE_ENTITY_UNSUPPORTED = "Custom fields are not available for that module.";

	public static final String MESSAGE_FIELD_LIMIT = "A module can have at most " + FIELD_LIMIT + " custom fields.";

	public static final String MESSAGE_OPTION_LIMIT = "A field can have at most " + OPTION_LIMIT + " choices.";

	public static final String MESSAGE_ALREADY_USED = "That value is already used by another record.";

	public static final String DETAIL_CONFIG = "config";

	public static final String DETAIL_OPTIONS = "options";

	public static final String DETAIL_UNIQUE = "unique";

	public static final String DETAIL_DEFAULT = "defaultValue";

	private CustomFieldConstants() {
	}

}

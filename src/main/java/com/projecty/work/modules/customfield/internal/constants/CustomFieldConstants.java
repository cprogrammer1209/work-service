package com.projecty.work.modules.customfield.internal.constants;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.projecty.work.modules.customfield.internal.entity.FieldType;
import com.projecty.work.modules.customfield.internal.entity.LayoutStatusType;

/**
 * Constants of the customfield module: limits, config keys, error codes and messages.
 */
public final class CustomFieldConstants {

	/** Only these types can be marked unique: a whole-record duplicate check makes no sense for the others. */
	public static final Set<FieldType> UNIQUE_TYPES = EnumSet.of(FieldType.TEXT, FieldType.NUMBER, FieldType.DECIMAL,
			FieldType.DATE);

	/** The modules that can have custom fields today. Others are added here when they exist. */
	public static final String ENTITY_PROJECT = "PROJECT";

	public static final String ENTITY_TASK = "TASK";

	public static final Set<String> ENTITY_TYPES = Set.of(ENTITY_PROJECT, ENTITY_TASK);

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

	/** CUS-BR-13: layouts per module on the default plan (inactive layouts count). */
	public static final int LAYOUT_LIMIT = 3;

	public static final int LAYOUT_NAME_MAX_LENGTH = 50;

	public static final int LAYOUT_DESCRIPTION_MAX_LENGTH = 300;

	public static final int SECTION_NAME_MAX_LENGTH = 50;

	/** The layout every portal starts with, and the section its fields start in. */
	public static final String DEFAULT_LAYOUT_NAME = "Standard";

	public static final String DEFAULT_LAYOUT_DESCRIPTION = "Every field in one section.";

	public static final String DEFAULT_SECTION_NAME = "Details";

	/** Who is recorded as the author of a layout the system creates the first time it is needed. */
	public static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

	public static final int STATUS_NAME_MAX_LENGTH = 30;

	public static final int STATUS_COLOR_MAX_LENGTH = 20;

	public static final int STATUS_LIMIT = 20;

	/** The statuses a new TASK layout starts with: name, type, color. The first one is the default. */
	public static final List<DefaultStatus> DEFAULT_STATUSES = List.of(
			new DefaultStatus("Open", LayoutStatusType.OPEN, "#2563eb"),
			new DefaultStatus("In Progress", LayoutStatusType.OPEN, "#d97706"),
			new DefaultStatus("Closed", LayoutStatusType.CLOSED, "#16a34a"));

	public static final String ERROR_STATUS_NOT_FOUND = "LAYOUT_STATUS_NOT_FOUND";

	public static final String ERROR_STATUS_INVALID = "LAYOUT_STATUS_INVALID";

	public static final String ERROR_STATUS_IN_USE = "LAYOUT_STATUS_IN_USE";

	public static final String ERROR_STATUS_LIMIT = "LAYOUT_STATUS_LIMIT";

	public static final String MESSAGE_STATUS_NOT_FOUND = "That status does not exist.";

	public static final String MESSAGE_STATUSES_UNSUPPORTED = "Only task layouts have statuses.";

	public static final String MESSAGE_STATUS_LIMIT = "A layout can have at most " + STATUS_LIMIT + " statuses.";

	public static final String MESSAGE_STATUS_NAME_REQUIRED = "Name the status.";

	public static final String MESSAGE_STATUS_NAME_TAKEN = "A status with this name already exists.";

	public static final String MESSAGE_STATUS_TYPE_REQUIRED = "Choose Open or Closed.";

	public static final String MESSAGE_STATUS_COLOR_TOO_LONG = "Use " + STATUS_COLOR_MAX_LENGTH
			+ " characters or fewer.";

	public static final String MESSAGE_STATUS_DEFAULT_OPEN = "The default status must be an Open one.";

	public static final String MESSAGE_STATUS_DEFAULT_UNSET = "Make another status the default first.";

	public static final String MESSAGE_STATUS_LAST_OF_TYPE = "A layout needs at least one Open and one Closed status.";

	public static final String MESSAGE_STATUS_IN_USE = "Tasks use this status. Move them to another status first.";

	public static final String MESSAGE_STATUS_TYPE_IN_USE = "Tasks use this status, so its type cannot change.";

	public static final String MESSAGE_STATUS_ORDER_INVALID = "List each status of the layout once.";

	public static final String DETAIL_STATUS_NAME = "name";

	public static final String DETAIL_STATUS_TYPE = "statusType";

	public static final String DETAIL_STATUS_COLOR = "color";

	public static final String DETAIL_STATUS_DEFAULT = "defaultStatus";

	public static final String DETAIL_STATUS_IDS = "ids";

	public static final String ERROR_LAYOUT_NOT_FOUND = "LAYOUT_NOT_FOUND";

	public static final String ERROR_LAYOUT_LIMIT = "LAYOUT_LIMIT";

	public static final String ERROR_LAYOUT_INVALID = "LAYOUT_INVALID";

	public static final String MESSAGE_LAYOUT_NOT_FOUND = "That layout does not exist.";

	public static final String MESSAGE_LAYOUT_LIMIT = "A module can have at most " + LAYOUT_LIMIT
			+ " layouts. Deactivate one you no longer use, or edit it instead.";

	public static final String MESSAGE_LAYOUT_NAME_TAKEN = "A layout with this name already exists.";

	public static final String MESSAGE_LAYOUT_NAME_REQUIRED = "Name the layout.";

	public static final String MESSAGE_SECTIONS_REQUIRED = "A layout needs at least one section.";

	public static final String MESSAGE_SECTION_NAME_REQUIRED = "Every section needs a name.";

	public static final String MESSAGE_LAYOUT_DEFAULT_INACTIVE = "Make another layout the default first.";

	public static final String MESSAGE_LAYOUT_ACTIVATE_FIRST = "Activate the layout first.";

	public static final String DETAIL_NAME = "name";

	public static final String DETAIL_SECTIONS = "sections";

	/** Fields of these types take the whole row of a form unless a layout says otherwise. */
	public static final Set<FieldType> WIDE_TYPES = EnumSet.of(FieldType.LONG_TEXT, FieldType.MULTI_PICKLIST);

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

	/** @param name the status name @param type OPEN or CLOSED @param color a CSS color for the chip */
	public record DefaultStatus(String name, LayoutStatusType type, String color) {
	}

	/**
	 * Cached definitions: {@code work:defs:{portalId}:{version}:{what}}. Any change to a field or layout bumps the
	 * portal's version ({@code work:defs:{portalId}:ver}), so older entries are never read again and just expire.
	 */
	public static final String DEFINITIONS_KEY_PREFIX = "work:defs:";

	public static final String DEFINITIONS_VERSION_SUFFIX = "ver";

	public static final String DEFINITIONS_UNVERSIONED = "0";

	public static final String DEFINITIONS_SEPARATOR = ":";

	public static final String DEFINITIONS_FIELDS = "fields:";

	public static final String DEFINITIONS_LAYOUT = "layout:";

	public static final String DEFINITIONS_STATUSES = "statuses:";

	/** Longer than any cached entry lives, so a reset version can never meet an entry written under the same number. */
	public static final Duration DEFINITIONS_VERSION_TTL = Duration.ofDays(1);

	private CustomFieldConstants() {
	}

}

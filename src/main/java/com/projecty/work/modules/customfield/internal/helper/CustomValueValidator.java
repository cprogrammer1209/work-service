package com.projecty.work.modules.customfield.internal.helper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.exception.AppException;
import com.projecty.work.modules.access.api.MemberAccessLookUpService;
import com.projecty.work.modules.access.api.dto.MemberAccessResponseDTO;
import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;
import com.projecty.work.modules.customfield.internal.entity.CustomField;
import com.projecty.work.modules.customfield.internal.entity.CustomFieldOption;
import com.projecty.work.modules.customfield.internal.entity.FieldType;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * The single gate for custom answers: every save, from every path, goes through {@link #validate}. It checks the
 * answer against the field's type and rules and returns it in the form we store (text, Long, BigDecimal, Boolean,
 * ISO date text, option or user ids). It also checks the rules of a field definition itself (config, defaults).
 */
@Component
public class CustomValueValidator {

	private static final String UNKNOWN_FIELD = "This field doesn't exist or isn't active.";

	private final MemberAccessLookUpService accessLookUpService;

	private final ObjectMapper objectMapper;

	public CustomValueValidator(MemberAccessLookUpService accessLookUpService, ObjectMapper objectMapper) {
		this.accessLookUpService = accessLookUpService;
		this.objectMapper = objectMapper;
	}

	/**
	 * @param fields the ACTIVE fields of the module
	 * @param input the keys to set; null or empty clears a key
	 * @param existing what the record stores now (keys of inactive fields are kept as they are)
	 * @param creating on create every active mandatory field must have an answer; on update only the keys sent
	 * @return the full cleaned map to store
	 */
	public Map<String, Object> validate(UUID portalId, List<CustomField> fields,
			Map<UUID, List<CustomFieldOption>> optionsByField, Map<String, Object> input,
			Map<String, Object> existing, boolean creating) {
		Map<String, CustomField> byKey = new LinkedHashMap<>();
		fields.forEach((field) -> byKey.put(field.getApiName(), field));
		Map<String, Object> problems = new LinkedHashMap<>();
		Map<String, Object> result = new LinkedHashMap<>(existing);
		for (Map.Entry<String, Object> entry : input.entrySet()) {
			String key = entry.getKey();
			CustomField field = byKey.get(key);
			if (field == null) {
				problems.put(key, UNKNOWN_FIELD);
				continue;
			}
			if (isEmpty(entry.getValue())) {
				if (field.isMandatory()) {
					problems.put(key, field.getLabel() + " is required.");
				}
				else {
					result.remove(key);
				}
				continue;
			}
			try {
				result.put(key, normalise(portalId, field.getFieldType(), field.getLabel(), field.getConfig(),
						optionsOf(optionsByField, field), entry.getValue(), existing.get(key)));
			}
			catch (ValueProblem problem) {
				problems.put(key, problem.getMessage());
			}
		}
		if (creating) {
			for (CustomField field : fields) {
				if (input.containsKey(field.getApiName())) {
					continue;
				}
				Object fallback = defaultFor(field, optionsOf(optionsByField, field));
				if (fallback != null) {
					result.put(field.getApiName(), fallback);
				}
				else if (field.isMandatory()) {
					problems.put(field.getApiName(), field.getLabel() + " is required.");
				}
			}
		}
		if (!problems.isEmpty()) {
			throw new AppException(HttpStatus.BAD_REQUEST, AppConstants.ERROR_VALIDATION_FAILED,
					AppConstants.MESSAGE_VALIDATION_FAILED, problems);
		}
		return result;
	}

	/** The answer a new record gets when none is sent, or null. */
	private Object defaultFor(CustomField field, List<CustomFieldOption> options) {
		switch (field.getFieldType()) {
			case PICKLIST:
				return options.stream()
					.filter((option) -> option.isActive() && option.isDefaultOption())
					.map((option) -> option.getId().toString())
					.findFirst()
					.orElse(null);
			case MULTI_PICKLIST:
				List<String> ids = options.stream()
					.filter((option) -> option.isActive() && option.isDefaultOption())
					.map((option) -> option.getId().toString())
					.toList();
				return ids.isEmpty() ? null : ids;
			case USER:
				return null;
			default:
				if (field.getDefaultValue() == null) {
					return null;
				}
				return normalise(null, field.getFieldType(), field.getLabel(), field.getConfig(), List.of(),
						this.objectMapper.readValue(field.getDefaultValue(), Object.class), null);
		}
	}

	/**
	 * Checks the default answer given when a field is defined and returns it as JSON text (null for none).
	 * Choices and people can't have a typed default: their defaults are the default choices.
	 */
	public String checkDefault(FieldType type, String label, Map<String, Object> config, Object raw) {
		if (isEmpty(raw)) {
			return null;
		}
		if (type.hasOptions() || type == FieldType.USER) {
			throw new ValueProblem(type.hasOptions() ? "Mark a choice as the default instead."
					: "A user field can't have a default.");
		}
		Object value = normalise(null, type, label, config, List.of(), raw, null);
		return this.objectMapper.writeValueAsString(value);
	}

	/** Checks the per-type rules of a field definition. */
	public void checkConfig(FieldType type, Map<String, Object> config) {
		Set<String> allowed = switch (type) {
			case TEXT -> Set.of(CustomFieldConstants.CONFIG_MAX_LENGTH, CustomFieldConstants.CONFIG_REGEX);
			case LONG_TEXT -> Set.of(CustomFieldConstants.CONFIG_MAX_LENGTH);
			case NUMBER -> Set.of(CustomFieldConstants.CONFIG_MIN, CustomFieldConstants.CONFIG_MAX);
			case DECIMAL -> Set.of(CustomFieldConstants.CONFIG_MIN, CustomFieldConstants.CONFIG_MAX,
					CustomFieldConstants.CONFIG_DECIMALS);
			case MULTI_PICKLIST -> Set.of(CustomFieldConstants.CONFIG_MAX_SELECTIONS);
			default -> Set.<String>of();
		};
		for (String key : config.keySet()) {
			if (!allowed.contains(key)) {
				throw new ValueProblem("'" + key + "' is not a setting of a " + type + " field.");
			}
		}
		int textMax = type == FieldType.LONG_TEXT ? CustomFieldConstants.LONG_TEXT_MAX_LENGTH
				: CustomFieldConstants.TEXT_MAX_LENGTH;
		requireInt(config, CustomFieldConstants.CONFIG_MAX_LENGTH, 1, textMax);
		requireInt(config, CustomFieldConstants.CONFIG_DECIMALS, 0, CustomFieldConstants.DECIMALS_MAX);
		requireInt(config, CustomFieldConstants.CONFIG_MAX_SELECTIONS, 1, CustomFieldConstants.SELECTIONS_MAX);
		Object regex = config.get(CustomFieldConstants.CONFIG_REGEX);
		if (regex != null) {
			if (!(regex instanceof String pattern) || pattern.length() > CustomFieldConstants.REGEX_MAX_LENGTH) {
				throw new ValueProblem("regex must be text of at most " + CustomFieldConstants.REGEX_MAX_LENGTH
						+ " characters.");
			}
			try {
				Pattern.compile(pattern);
			}
			catch (PatternSyntaxException ex) {
				throw new ValueProblem("regex is not a valid pattern.");
			}
		}
		BigDecimal min = configNumber(config, CustomFieldConstants.CONFIG_MIN);
		BigDecimal max = configNumber(config, CustomFieldConstants.CONFIG_MAX);
		for (String key : List.of(CustomFieldConstants.CONFIG_MIN, CustomFieldConstants.CONFIG_MAX)) {
			if (config.containsKey(key) && !(config.get(key) instanceof Number)) {
				throw new ValueProblem(key + " must be a number.");
			}
		}
		if (min != null && max != null && min.compareTo(max) > 0) {
			throw new ValueProblem("min can't be greater than max.");
		}
	}

	/** The keys whose answers differ. Numbers compare by value, not by the Java type JSON gave them. */
	public Set<String> changedKeys(Map<String, Object> before, Map<String, Object> after) {
		Set<String> keys = new LinkedHashSet<>(before.keySet());
		keys.addAll(after.keySet());
		Set<String> changed = new LinkedHashSet<>();
		for (String key : keys) {
			if (!sameValue(before.get(key), after.get(key))) {
				changed.add(key);
			}
		}
		return changed;
	}

	/** The text a unique field reserves for an answer (case-insensitive), or null for no answer. */
	public String uniqueKey(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
		}
		return String.valueOf(value).trim().toLowerCase(Locale.ROOT);
	}

	private boolean sameValue(Object a, Object b) {
		if (a instanceof Number x && b instanceof Number y) {
			return new BigDecimal(x.toString()).compareTo(new BigDecimal(y.toString())) == 0;
		}
		return Objects.equals(a, b);
	}

	private Object normalise(UUID portalId, FieldType type, String label, Map<String, Object> config,
			List<CustomFieldOption> options, Object raw, Object current) {
		return switch (type) {
			case TEXT -> text(label, config, raw, CustomFieldConstants.TEXT_MAX_LENGTH);
			case LONG_TEXT -> text(label, config, raw, CustomFieldConstants.LONG_TEXT_MAX_LENGTH);
			case NUMBER -> number(label, config, raw, true);
			case DECIMAL -> number(label, config, raw, false);
			case DATE -> date(label, raw);
			case CHECKBOX -> checkbox(label, raw);
			case PICKLIST -> choice(label, options, raw, current);
			case MULTI_PICKLIST -> choices(label, config, options, raw, current);
			case USER -> user(portalId, label, raw);
		};
	}

	private String text(String label, Map<String, Object> config, Object raw, int limit) {
		if (!(raw instanceof String value)) {
			throw new ValueProblem(label + " must be text.");
		}
		String trimmed = value.trim();
		int max = Math.min(configInt(config, CustomFieldConstants.CONFIG_MAX_LENGTH, limit), limit);
		if (trimmed.length() > max) {
			throw new ValueProblem(label + " must be at most " + max + " characters.");
		}
		if (config.get(CustomFieldConstants.CONFIG_REGEX) instanceof String regex
				&& !Pattern.matches(regex, trimmed)) {
			throw new ValueProblem(label + " doesn't match the required format.");
		}
		return trimmed;
	}

	private Object number(String label, Map<String, Object> config, Object raw, boolean whole) {
		BigDecimal value = toDecimal(label, raw);
		if (value.abs().compareTo(CustomFieldConstants.NUMBER_MAX_ABS) > 0) {
			throw new ValueProblem(label + " must be between -" + CustomFieldConstants.NUMBER_MAX_ABS + " and "
					+ CustomFieldConstants.NUMBER_MAX_ABS + ".");
		}
		BigDecimal stripped = value.stripTrailingZeros();
		int scale = Math.max(stripped.scale(), 0);
		int decimals = configInt(config, CustomFieldConstants.CONFIG_DECIMALS, CustomFieldConstants.DECIMALS_DEFAULT);
		if (whole && scale > 0) {
			throw new ValueProblem(label + " must be a whole number.");
		}
		if (!whole && scale > decimals) {
			throw new ValueProblem(label + " can have at most " + decimals + " decimal places.");
		}
		BigDecimal min = configNumber(config, CustomFieldConstants.CONFIG_MIN);
		BigDecimal max = configNumber(config, CustomFieldConstants.CONFIG_MAX);
		if (min != null && value.compareTo(min) < 0) {
			throw new ValueProblem(label + " must be at least " + min.toPlainString() + ".");
		}
		if (max != null && value.compareTo(max) > 0) {
			throw new ValueProblem(label + " must be at most " + max.toPlainString() + ".");
		}
		BigDecimal plain = stripped.setScale(scale);
		return whole ? (Object) plain.longValueExact() : plain;
	}

	private BigDecimal toDecimal(String label, Object raw) {
		try {
			if (raw instanceof Number number) {
				return new BigDecimal(number.toString());
			}
			if (raw instanceof String text) {
				return new BigDecimal(text.trim());
			}
		}
		catch (NumberFormatException ex) {
			// falls through to the message below
		}
		throw new ValueProblem(label + " must be a number.");
	}

	private String date(String label, Object raw) {
		if (raw instanceof String text) {
			try {
				return LocalDate.parse(text.trim()).toString();
			}
			catch (DateTimeParseException ex) {
				// falls through to the message below
			}
		}
		throw new ValueProblem(label + " must be a date like 2026-12-01.");
	}

	private Boolean checkbox(String label, Object raw) {
		if (raw instanceof Boolean value) {
			return value;
		}
		throw new ValueProblem(label + " must be true or false.");
	}

	private String choice(String label, List<CustomFieldOption> options, Object raw, Object current) {
		UUID id = parseId(label, raw);
		CustomFieldOption option = options.stream()
			.filter((candidate) -> candidate.getId().equals(id))
			.findFirst()
			.orElseThrow(() -> new ValueProblem(label + " has a choice that doesn't exist."));
		if (!option.isActive() && !holds(current, id.toString())) {
			throw new ValueProblem("The choice \"" + option.getLabel() + "\" is no longer available.");
		}
		return id.toString();
	}

	private List<String> choices(String label, Map<String, Object> config, List<CustomFieldOption> options, Object raw,
			Object current) {
		if (!(raw instanceof Collection<?> values)) {
			throw new ValueProblem(label + " must be a list of choices.");
		}
		int max = configInt(config, CustomFieldConstants.CONFIG_MAX_SELECTIONS, CustomFieldConstants.SELECTIONS_DEFAULT);
		if (values.size() > max) {
			throw new ValueProblem(label + " allows at most " + max + " choices.");
		}
		List<String> ids = new ArrayList<>();
		for (Object value : values) {
			String id = choice(label, options, value, current);
			if (ids.contains(id)) {
				throw new ValueProblem(label + " has the same choice twice.");
			}
			ids.add(id);
		}
		return ids;
	}

	private String user(UUID portalId, String label, Object raw) {
		UUID id = parseId(label, raw);
		boolean active = this.accessLookUpService.findMemberAccess(portalId, id)
			.filter(MemberAccessResponseDTO::active)
			.isPresent();
		if (!active) {
			throw new ValueProblem(label + " must be an active person in this portal.");
		}
		return id.toString();
	}

	private UUID parseId(String label, Object raw) {
		if (raw instanceof String text) {
			try {
				return UUID.fromString(text.trim());
			}
			catch (IllegalArgumentException ex) {
				// falls through to the message below
			}
		}
		throw new ValueProblem(label + " has an invalid value.");
	}

	/** True when the stored answer (a single id or a list of ids) already holds this id. */
	private boolean holds(Object current, String id) {
		if (current instanceof Collection<?> values) {
			return values.stream().anyMatch((value) -> id.equals(String.valueOf(value)));
		}
		return current != null && id.equals(String.valueOf(current));
	}

	private static boolean isEmpty(Object value) {
		return value == null || (value instanceof String text && text.isBlank())
				|| (value instanceof Collection<?> values && values.isEmpty());
	}

	private static List<CustomFieldOption> optionsOf(Map<UUID, List<CustomFieldOption>> optionsByField,
			CustomField field) {
		return optionsByField.getOrDefault(field.getId(), List.of());
	}

	private static int configInt(Map<String, Object> config, String key, int fallback) {
		return config.get(key) instanceof Number number ? number.intValue() : fallback;
	}

	private static BigDecimal configNumber(Map<String, Object> config, String key) {
		return config.get(key) instanceof Number number ? new BigDecimal(number.toString()) : null;
	}

	private static void requireInt(Map<String, Object> config, String key, int min, int max) {
		Object value = config.get(key);
		if (value == null) {
			return;
		}
		if (!(value instanceof Number number) || number.doubleValue() != Math.rint(number.doubleValue())
				|| number.intValue() < min || number.intValue() > max) {
			throw new ValueProblem(key + " must be a whole number from " + min + " to " + max + ".");
		}
	}

}

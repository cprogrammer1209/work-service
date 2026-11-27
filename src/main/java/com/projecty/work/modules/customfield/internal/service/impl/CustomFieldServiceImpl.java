package com.projecty.work.modules.customfield.internal.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.exception.AppException;
import com.projecty.work.common.service.RequestPortalContext;
import com.projecty.work.modules.customfield.internal.helper.DefinitionCache;
import com.projecty.work.modules.outbox.api.OutboxWriteService;
import com.projecty.work.modules.outbox.api.dto.OutboxEventCreateDTO;
import com.projecty.work.modules.shared.constants.OutboxEventConstants;
import com.projecty.work.modules.customfield.api.CustomValueHolder;
import com.projecty.work.modules.customfield.api.dto.CustomFieldResponseDTO;
import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldCreateDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldOptionCreateDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldOptionUpdateDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldReorderDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldUpdateDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldUsageResponseDTO;
import com.projecty.work.modules.customfield.internal.entity.CustomField;
import com.projecty.work.modules.customfield.internal.entity.CustomFieldOption;
import com.projecty.work.modules.customfield.internal.entity.CustomFieldUniqueValue;
import com.projecty.work.modules.customfield.internal.entity.FieldStatus;
import com.projecty.work.modules.customfield.internal.entity.FieldType;
import com.projecty.work.modules.customfield.internal.helper.ApiNameGenerator;
import com.projecty.work.modules.customfield.internal.helper.CustomValueValidator;
import com.projecty.work.modules.customfield.internal.helper.FieldRule;
import com.projecty.work.modules.customfield.internal.helper.ValueProblem;
import com.projecty.work.modules.customfield.internal.mapper.CustomFieldMapper;
import com.projecty.work.modules.customfield.internal.repository.CustomFieldOptionRepository;
import com.projecty.work.modules.customfield.internal.repository.CustomFieldRepository;
import com.projecty.work.modules.customfield.internal.repository.CustomFieldUniqueValueRepository;
import com.projecty.work.modules.customfield.internal.service.CustomFieldService;
import com.projecty.work.modules.customfield.internal.service.LayoutService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class CustomFieldServiceImpl implements CustomFieldService {

	private final CustomFieldRepository fieldRepository;

	private final CustomFieldOptionRepository optionRepository;

	private final CustomFieldUniqueValueRepository uniqueRepository;

	private final CustomFieldMapper mapper;

	private final CustomValueValidator validator;

	private final ApiNameGenerator apiNames;

	private final LayoutService layoutService;

	private final List<CustomValueHolder> holders;

	private final OutboxWriteService outboxWriteService;

	private final RequestPortalContext portalContext;

	private final ObjectMapper objectMapper;

	private final DefinitionCache definitionCache;

	private final Clock clock;

	public CustomFieldServiceImpl(CustomFieldRepository fieldRepository, CustomFieldOptionRepository optionRepository,
			CustomFieldUniqueValueRepository uniqueRepository, CustomFieldMapper mapper,
			CustomValueValidator validator, ApiNameGenerator apiNames, LayoutService layoutService,
			List<CustomValueHolder> holders,
			OutboxWriteService outboxWriteService, RequestPortalContext portalContext, ObjectMapper objectMapper,
			DefinitionCache definitionCache, Clock clock) {
		this.fieldRepository = fieldRepository;
		this.optionRepository = optionRepository;
		this.uniqueRepository = uniqueRepository;
		this.mapper = mapper;
		this.validator = validator;
		this.apiNames = apiNames;
		this.layoutService = layoutService;
		this.holders = holders;
		this.outboxWriteService = outboxWriteService;
		this.portalContext = portalContext;
		this.objectMapper = objectMapper;
		this.definitionCache = definitionCache;
		this.clock = clock;
	}

	/* ---------- the ports other modules use ---------- */

	@Override
	@Transactional(readOnly = true)
	public List<CustomFieldResponseDTO> findFields(UUID portalId, String entityType) {
		return this.definitionCache.read(portalId, CustomFieldConstants.DEFINITIONS_FIELDS + entityType,
				new TypeReference<List<CustomFieldResponseDTO>>() {
				}, () -> respond(portalId, load(portalId, entityType, EnumSet.of(FieldStatus.ACTIVE))));
	}

	@Override
	public String definitionsVersion(UUID portalId) {
		return this.definitionCache.version(portalId);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> validate(UUID portalId, String entityType, UUID layoutId, Map<String, Object> input,
			Map<String, Object> existing, boolean creating) {
		List<FieldRule> rules = this.layoutService.rulesFor(portalId, entityType, layoutId);
		List<CustomField> fields = rules.stream().map(FieldRule::field).toList();
		return this.validator.validate(portalId, rules, optionsByField(portalId, fields), input, existing, creating);
	}

	@Override
	public Set<String> changedKeys(Map<String, Object> before, Map<String, Object> after) {
		return this.validator.changedKeys(before, after);
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void reserveUniqueValues(UUID portalId, String entityType, UUID entityId, Map<String, Object> before,
			Map<String, Object> after) {
		Map<String, Object> problems = new LinkedHashMap<>();
		for (CustomField field : load(portalId, entityType, EnumSet.of(FieldStatus.ACTIVE))) {
			if (!field.isUniqueField()) {
				continue;
			}
			String oldKey = this.validator.uniqueKey(before.get(field.getApiName()));
			String newKey = this.validator.uniqueKey(after.get(field.getApiName()));
			if (java.util.Objects.equals(oldKey, newKey)) {
				continue;
			}
			if (oldKey != null) {
				this.uniqueRepository.deleteByPortalIdAndFieldIdAndEntityId(portalId, field.getId(), entityId);
				this.uniqueRepository.flush();
			}
			if (newKey == null) {
				continue;
			}
			boolean taken = this.uniqueRepository.existsByPortalIdAndFieldIdAndValueKey(portalId, field.getId(), newKey);
			if (!taken) {
				try {
					this.uniqueRepository.saveAndFlush(new CustomFieldUniqueValue(portalId, field.getId(), newKey, entityId));
				}
				catch (DataIntegrityViolationException ex) {
					taken = true;
				}
			}
			if (taken) {
				problems.put(field.getApiName(), field.getLabel() + ": " + CustomFieldConstants.MESSAGE_ALREADY_USED);
			}
		}
		if (!problems.isEmpty()) {
			throw new AppException(HttpStatus.BAD_REQUEST, AppConstants.ERROR_VALIDATION_FAILED,
					AppConstants.MESSAGE_VALIDATION_FAILED, problems);
		}
	}

	/* ---------- the admin screens ---------- */

	@Override
	@Transactional(readOnly = true)
	public List<CustomFieldResponseDTO> listActive(String entityType) {
		UUID portalId = this.portalContext.requirePortalId();
		return respond(portalId, load(portalId, requireEntityType(entityType), EnumSet.of(FieldStatus.ACTIVE)));
	}

	@Override
	@Transactional(readOnly = true)
	public List<CustomFieldResponseDTO> listAll(String entityType) {
		UUID portalId = this.portalContext.requirePortalId();
		return respond(portalId, load(portalId, requireEntityType(entityType),
				EnumSet.of(FieldStatus.ACTIVE, FieldStatus.INACTIVE)));
	}

	@Override
	@Transactional
	public CustomFieldResponseDTO create(UUID actorId, String actorName, CustomFieldCreateDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		String entityType = requireEntityType(request.entityType());
		if (this.fieldRepository.countByPortalIdAndEntityTypeAndStatusNot(portalId, entityType,
				FieldStatus.DELETED) >= CustomFieldConstants.FIELD_LIMIT) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, CustomFieldConstants.ERROR_FIELD_LIMIT,
					CustomFieldConstants.MESSAGE_FIELD_LIMIT);
		}
		FieldType type = request.fieldType();
		String label = request.label().trim();
		Map<String, Object> config = request.config() == null ? Map.of() : request.config();
		List<CustomFieldOptionCreateDTO> options = request.options() == null ? List.of() : request.options();
		if (!options.isEmpty() && !type.hasOptions()) {
			throw invalid(CustomFieldConstants.DETAIL_OPTIONS, "Only a picklist or multi-select has choices.");
		}
		if (options.size() > CustomFieldConstants.OPTION_LIMIT) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, CustomFieldConstants.ERROR_OPTION_LIMIT,
					CustomFieldConstants.MESSAGE_OPTION_LIMIT);
		}
		if (type == FieldType.PICKLIST && options.stream().filter(CustomFieldOptionCreateDTO::defaultOption).count() > 1) {
			throw invalid(CustomFieldConstants.DETAIL_OPTIONS, "A picklist can have only one default choice.");
		}
		if (request.unique() && !CustomFieldConstants.UNIQUE_TYPES.contains(type)) {
			throw invalid(CustomFieldConstants.DETAIL_UNIQUE, "Only text, number, decimal and date fields can be unique.");
		}
		String defaultJson = checked(CustomFieldConstants.DETAIL_CONFIG, () -> {
			this.validator.checkConfig(type, config);
			return null;
		}) == null ? checked(CustomFieldConstants.DETAIL_DEFAULT,
				() -> this.validator.checkDefault(type, label, config, request.defaultValue())) : null;
		Instant now = this.clock.instant();
		int sortOrder = this.fieldRepository.findFirstByPortalIdAndEntityTypeOrderBySortOrderDesc(portalId, entityType)
			.map((last) -> last.getSortOrder() + 1)
			.orElse(1);
		CustomField field = new CustomField(portalId, entityType, this.apiNames.generate(portalId, entityType, label),
				label, clean(request.description()), type, request.mandatory(), request.unique(), request.sensitive(),
				defaultJson, config, sortOrder, actorId, now);
		try {
			this.fieldRepository.saveAndFlush(field);
		}
		catch (DataIntegrityViolationException ex) {
			throw new AppException(HttpStatus.CONFLICT, CustomFieldConstants.ERROR_FIELD_INVALID,
					"A field with that key was just created. Try again.");
		}
		int position = 1;
		for (CustomFieldOptionCreateDTO option : options) {
			this.optionRepository.save(new CustomFieldOption(portalId, field.getId(), option.label().trim(),
					option.color(), position++, option.defaultOption()));
		}
		// A new field lands at the end of the default layout's last section; an Admin can move it from the layout builder.
		this.layoutService.placeNewField(portalId, field);
		publish(OutboxEventConstants.EVENT_FIELD_CREATED, field, actorId, actorName);
		return respondOne(portalId, field);
	}

	@Override
	@Transactional
	public CustomFieldResponseDTO update(UUID actorId, String actorName, UUID fieldId, CustomFieldUpdateDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		CustomField field = requireField(portalId, fieldId);
		String label = request.label().trim();
		Map<String, Object> config = request.config() == null ? Map.of() : request.config();
		checked(CustomFieldConstants.DETAIL_CONFIG, () -> {
			this.validator.checkConfig(field.getFieldType(), config);
			return null;
		});
		String defaultJson = checked(CustomFieldConstants.DETAIL_DEFAULT,
				() -> this.validator.checkDefault(field.getFieldType(), label, config, request.defaultValue()));
		field.edit(label, clean(request.description()), request.mandatory(), request.sensitive(), defaultJson, config,
				actorId, this.clock.instant());
		publish(OutboxEventConstants.EVENT_FIELD_UPDATED, field, actorId, actorName);
		return respondOne(portalId, field);
	}

	@Override
	@Transactional
	public CustomFieldResponseDTO deactivate(UUID actorId, String actorName, UUID fieldId) {
		UUID portalId = this.portalContext.requirePortalId();
		CustomField field = requireField(portalId, fieldId);
		if (field.getStatus() == FieldStatus.ACTIVE) {
			field.changeStatus(FieldStatus.INACTIVE, actorId, this.clock.instant());
			publish(OutboxEventConstants.EVENT_FIELD_DEACTIVATED, field, actorId, actorName);
		}
		return respondOne(portalId, field);
	}

	@Override
	@Transactional
	public CustomFieldResponseDTO activate(UUID actorId, String actorName, UUID fieldId) {
		UUID portalId = this.portalContext.requirePortalId();
		CustomField field = requireField(portalId, fieldId);
		if (field.getStatus() == FieldStatus.INACTIVE) {
			field.changeStatus(FieldStatus.ACTIVE, actorId, this.clock.instant());
			publish(OutboxEventConstants.EVENT_FIELD_UPDATED, field, actorId, actorName);
		}
		return respondOne(portalId, field);
	}

	@Override
	@Transactional(readOnly = true)
	public CustomFieldUsageResponseDTO usage(UUID fieldId) {
		UUID portalId = this.portalContext.requirePortalId();
		CustomField field = requireField(portalId, fieldId);
		long records = this.holders.stream()
			.filter((holder) -> holder.entityType().equals(field.getEntityType()))
			.mapToLong((holder) -> holder.countUsing(portalId, field.getApiName()))
			.sum();
		return new CustomFieldUsageResponseDTO(records);
	}

	@Override
	@Transactional
	public void delete(UUID actorId, String actorName, UUID fieldId) {
		UUID portalId = this.portalContext.requirePortalId();
		CustomField field = requireField(portalId, fieldId);
		field.changeStatus(FieldStatus.DELETED, actorId, this.clock.instant());
		// Answers are removed through the module that owns them; responses ignore keys without an active field, so
		// nothing leaks while this runs.
		this.holders.stream()
			.filter((holder) -> holder.entityType().equals(field.getEntityType()))
			.forEach((holder) -> holder.removeKey(portalId, field.getApiName()));
		this.uniqueRepository.deleteByPortalIdAndFieldId(portalId, field.getId());
		this.layoutService.removePlacements(portalId, field.getId());
		publish(OutboxEventConstants.EVENT_FIELD_DELETED, field, actorId, actorName);
	}

	@Override
	@Transactional
	public List<CustomFieldResponseDTO> reorder(CustomFieldReorderDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		String entityType = requireEntityType(request.entityType());
		Set<UUID> listed = new LinkedHashSet<>(request.fieldIds());
		int position = 1;
		for (UUID fieldId : listed) {
			CustomField field = requireField(portalId, fieldId);
			if (!field.getEntityType().equals(entityType)) {
				throw notFound();
			}
			field.moveTo(position++);
		}
		// Fields not in the list follow, in their current order, so positions never collide.
		for (CustomField other : load(portalId, entityType, EnumSet.of(FieldStatus.ACTIVE, FieldStatus.INACTIVE))) {
			if (!listed.contains(other.getId())) {
				other.moveTo(position++);
			}
		}
		this.fieldRepository.flush();
		// Reordering publishes no event, so it moves the definitions version itself: the order is part of what is cached.
		this.definitionCache.bumpAfterCommit(portalId);
		return respond(portalId, load(portalId, entityType, EnumSet.of(FieldStatus.ACTIVE, FieldStatus.INACTIVE)));
	}

	@Override
	@Transactional
	public CustomFieldResponseDTO addOption(UUID actorId, String actorName, UUID fieldId,
			CustomFieldOptionCreateDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		CustomField field = requireOptionField(portalId, fieldId);
		List<CustomFieldOption> existing = this.optionRepository.findByPortalIdAndFieldIdOrderBySortOrderAsc(portalId,
				field.getId());
		if (existing.size() >= CustomFieldConstants.OPTION_LIMIT) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, CustomFieldConstants.ERROR_OPTION_LIMIT,
					CustomFieldConstants.MESSAGE_OPTION_LIMIT);
		}
		if (request.defaultOption() && field.getFieldType() == FieldType.PICKLIST) {
			existing.forEach(CustomFieldOption::clearDefault);
		}
		int position = existing.stream().mapToInt(CustomFieldOption::getSortOrder).max().orElse(0) + 1;
		this.optionRepository.save(new CustomFieldOption(portalId, field.getId(), request.label().trim(),
				request.color(), position, request.defaultOption()));
		field.changeStatus(field.getStatus(), actorId, this.clock.instant());
		publish(OutboxEventConstants.EVENT_FIELD_UPDATED, field, actorId, actorName);
		return respondOne(portalId, field);
	}

	@Override
	@Transactional
	public CustomFieldResponseDTO updateOption(UUID actorId, String actorName, UUID fieldId, UUID optionId,
			CustomFieldOptionUpdateDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		CustomField field = requireOptionField(portalId, fieldId);
		CustomFieldOption option = this.optionRepository.findByIdAndPortalIdAndFieldId(optionId, portalId, field.getId())
			.orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, CustomFieldConstants.ERROR_OPTION_NOT_FOUND,
					CustomFieldConstants.MESSAGE_OPTION_NOT_FOUND));
		if (request.defaultOption() && field.getFieldType() == FieldType.PICKLIST) {
			this.optionRepository.findByPortalIdAndFieldIdOrderBySortOrderAsc(portalId, field.getId())
				.forEach(CustomFieldOption::clearDefault);
		}
		option.edit(request.label().trim(), request.color(), request.defaultOption(), request.active());
		field.changeStatus(field.getStatus(), actorId, this.clock.instant());
		publish(OutboxEventConstants.EVENT_FIELD_UPDATED, field, actorId, actorName);
		return respondOne(portalId, field);
	}

	/* ---------- helpers ---------- */

	private List<CustomField> load(UUID portalId, String entityType, Set<FieldStatus> statuses) {
		return this.fieldRepository.findByPortalIdAndEntityTypeAndStatusInOrderBySortOrderAscCreatedAtAsc(portalId,
				entityType, statuses);
	}

	private Map<UUID, List<CustomFieldOption>> optionsByField(UUID portalId, List<CustomField> fields) {
		if (fields.isEmpty()) {
			return Map.of();
		}
		return this.optionRepository
			.findByPortalIdAndFieldIdInOrderBySortOrderAsc(portalId, fields.stream().map(CustomField::getId).toList())
			.stream()
			.collect(Collectors.groupingBy(CustomFieldOption::getFieldId));
	}

	private List<CustomFieldResponseDTO> respond(UUID portalId, List<CustomField> fields) {
		Map<UUID, List<CustomFieldOption>> options = optionsByField(portalId, fields);
		return fields.stream()
			.map((field) -> this.mapper.toResponse(field, options.getOrDefault(field.getId(), List.of())))
			.toList();
	}

	private CustomFieldResponseDTO respondOne(UUID portalId, CustomField field) {
		return this.mapper.toResponse(field,
				this.optionRepository.findByPortalIdAndFieldIdOrderBySortOrderAsc(portalId, field.getId()));
	}

	/** A deleted field is gone as far as callers are concerned. Another portal's field is "not found" too. */
	private CustomField requireField(UUID portalId, UUID fieldId) {
		return this.fieldRepository.findByIdAndPortalId(fieldId, portalId)
			.filter((field) -> field.getStatus() != FieldStatus.DELETED)
			.orElseThrow(this::notFound);
	}

	private CustomField requireOptionField(UUID portalId, UUID fieldId) {
		CustomField field = requireField(portalId, fieldId);
		if (!field.getFieldType().hasOptions()) {
			throw invalid(CustomFieldConstants.DETAIL_OPTIONS, "Only a picklist or multi-select has choices.");
		}
		return field;
	}

	private static String requireEntityType(String entityType) {
		if (entityType == null || !CustomFieldConstants.ENTITY_TYPES.contains(entityType)) {
			throw new AppException(HttpStatus.BAD_REQUEST, CustomFieldConstants.ERROR_ENTITY_UNSUPPORTED,
					CustomFieldConstants.MESSAGE_ENTITY_UNSUPPORTED);
		}
		return entityType;
	}

	/** Runs a check of the field definition and turns a {@link ValueProblem} into a field-level validation error. */
	private static <T> T checked(String detail, Supplier<T> check) {
		try {
			return check.get();
		}
		catch (ValueProblem problem) {
			throw invalid(detail, problem.getMessage());
		}
	}

	private static AppException invalid(String detail, String message) {
		Map<String, Object> details = new HashMap<>();
		details.put(detail, message);
		return new AppException(HttpStatus.BAD_REQUEST, AppConstants.ERROR_VALIDATION_FAILED,
				AppConstants.MESSAGE_VALIDATION_FAILED, details);
	}

	private AppException notFound() {
		return new AppException(HttpStatus.NOT_FOUND, CustomFieldConstants.ERROR_FIELD_NOT_FOUND,
				CustomFieldConstants.MESSAGE_FIELD_NOT_FOUND);
	}

	private static String clean(String text) {
		return text == null || text.isBlank() ? null : text.trim();
	}

	/** Same transaction as the change; the relay sends it to Kafka afterwards (never for a rolled-back change). */
	private void publish(String eventType, CustomField field, UUID actorId, String actorName) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("portalId", field.getPortalId());
		payload.put("fieldId", field.getId());
		payload.put("entityType", field.getEntityType());
		payload.put("apiName", field.getApiName());
		this.outboxWriteService.publish(new OutboxEventCreateDTO(field.getPortalId(),
				OutboxEventConstants.AGGREGATE_FIELD, field.getId(), eventType, actorId, actorName, payload));
		this.definitionCache.bumpAfterCommit(field.getPortalId());
	}

}

package com.projecty.work.modules.customfield.internal.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.exception.AppException;
import com.projecty.work.common.service.RequestPortalContext;
import com.projecty.work.modules.customfield.api.CustomValueHolder;
import com.projecty.work.modules.customfield.api.dto.LayoutFieldResponseDTO;
import com.projecty.work.modules.customfield.api.dto.LayoutResponseDTO;
import com.projecty.work.modules.customfield.api.dto.LayoutSectionResponseDTO;
import com.projecty.work.modules.customfield.api.dto.LayoutStatusResponseDTO;
import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;
import com.projecty.work.modules.customfield.internal.dto.LayoutCreateDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutEditorResponseDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutFieldUpdateDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutPlacementResponseDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutSectionEditorResponseDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutSectionUpdateDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutSummaryResponseDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutUpdateDTO;
import com.projecty.work.modules.customfield.internal.entity.CustomField;
import com.projecty.work.modules.customfield.internal.entity.CustomFieldOption;
import com.projecty.work.modules.customfield.internal.entity.FieldStatus;
import com.projecty.work.modules.customfield.internal.entity.Layout;
import com.projecty.work.modules.customfield.internal.helper.LayoutStatusSeeder;
import com.projecty.work.modules.customfield.internal.repository.LayoutStatusRepository;
import com.projecty.work.modules.customfield.internal.entity.LayoutField;
import com.projecty.work.modules.customfield.internal.entity.LayoutSection;
import com.projecty.work.modules.customfield.internal.helper.FieldRule;
import com.projecty.work.modules.customfield.internal.helper.ValueProblem;
import com.projecty.work.modules.customfield.internal.helper.CustomValueValidator;
import com.projecty.work.modules.customfield.internal.mapper.CustomFieldMapper;
import com.projecty.work.modules.customfield.internal.mapper.LayoutMapper;
import com.projecty.work.modules.customfield.internal.repository.CustomFieldOptionRepository;
import com.projecty.work.modules.customfield.internal.repository.CustomFieldRepository;
import com.projecty.work.modules.customfield.internal.repository.LayoutFieldRepository;
import com.projecty.work.modules.customfield.internal.repository.LayoutRepository;
import com.projecty.work.modules.customfield.internal.repository.LayoutSectionRepository;
import com.projecty.work.modules.customfield.internal.service.LayoutService;
import com.projecty.work.modules.outbox.api.OutboxWriteService;
import com.projecty.work.modules.outbox.api.dto.OutboxEventCreateDTO;
import com.projecty.work.modules.shared.constants.OutboxEventConstants;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LayoutServiceImpl implements LayoutService {

	private final LayoutRepository layoutRepository;

	private final LayoutSectionRepository sectionRepository;

	private final LayoutFieldRepository placementRepository;

	private final CustomFieldRepository fieldRepository;

	private final CustomFieldOptionRepository optionRepository;

	private final CustomFieldMapper fieldMapper;

	private final LayoutMapper layoutMapper;

	private final CustomValueValidator validator;

	private final LayoutStatusRepository statusRepository;

	private final LayoutStatusSeeder statusSeeder;

	private final List<CustomValueHolder> holders;

	private final OutboxWriteService outboxWriteService;

	private final RequestPortalContext portalContext;

	private final Clock clock;

	public LayoutServiceImpl(LayoutRepository layoutRepository, LayoutSectionRepository sectionRepository,
			LayoutFieldRepository placementRepository, CustomFieldRepository fieldRepository,
			CustomFieldOptionRepository optionRepository, CustomFieldMapper fieldMapper, LayoutMapper layoutMapper,
			CustomValueValidator validator, LayoutStatusRepository statusRepository,
			LayoutStatusSeeder statusSeeder, List<CustomValueHolder> holders, OutboxWriteService outboxWriteService,
			RequestPortalContext portalContext, Clock clock) {
		this.layoutRepository = layoutRepository;
		this.sectionRepository = sectionRepository;
		this.placementRepository = placementRepository;
		this.fieldRepository = fieldRepository;
		this.optionRepository = optionRepository;
		this.fieldMapper = fieldMapper;
		this.layoutMapper = layoutMapper;
		this.validator = validator;
		this.statusRepository = statusRepository;
		this.statusSeeder = statusSeeder;
		this.holders = holders;
		this.outboxWriteService = outboxWriteService;
		this.portalContext = portalContext;
		this.clock = clock;
	}

	/** Everything a set of layouts is made of, loaded together. */
	private record Graph(Map<UUID, List<LayoutSection>> sections, Map<UUID, List<LayoutField>> placements,
			Map<UUID, CustomField> fields, Map<UUID, List<CustomFieldOption>> options) {
	}

	/* ---------- the port other modules use ---------- */

	/** Not read-only: the first call for a module creates its default layout. */
	@Override
	@Transactional
	public List<LayoutResponseDTO> findActiveLayouts(UUID portalId, String entityType) {
		ensureDefault(portalId, entityType);
		List<Layout> layouts = this.layoutRepository.findByPortalIdAndEntityTypeOrderByCreatedAtAscIdAsc(portalId, entityType)
			.stream()
			.filter(Layout::isActive)
			.sorted((a, b) -> Boolean.compare(b.isDefaultLayout(), a.isDefaultLayout()))
			.toList();
		Graph graph = load(portalId, layouts);
		return layouts.stream().map((layout) -> resolve(layout, graph)).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public LayoutResponseDTO getLayout(UUID portalId, UUID layoutId) {
		Layout layout = requireLayout(portalId, layoutId);
		return resolve(layout, load(portalId, List.of(layout)));
	}

	@Override
	@Transactional
	public UUID getDefaultLayoutId(UUID portalId, String entityType) {
		return ensureDefault(portalId, entityType).getId();
	}

	@Override
	@Transactional(readOnly = true)
	public List<LayoutStatusResponseDTO> findStatuses(UUID portalId, UUID layoutId) {
		Layout layout = requireLayout(portalId, layoutId);
		return statusesOf(layout);
	}

	@Override
	@Transactional(readOnly = true)
	public LayoutStatusResponseDTO getStatus(UUID portalId, UUID layoutId, UUID statusId) {
		requireLayout(portalId, layoutId);
		return this.statusRepository.findByIdAndPortalIdAndLayoutId(statusId, portalId, layoutId)
			.map(this.layoutMapper::toStatus)
			.orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, CustomFieldConstants.ERROR_STATUS_NOT_FOUND,
					CustomFieldConstants.MESSAGE_STATUS_NOT_FOUND));
	}

	/* ---------- what the admin screens use ---------- */

	@Override
	@Transactional
	public List<LayoutResponseDTO> listActive(String entityType) {
		return findActiveLayouts(this.portalContext.requirePortalId(), requireEntityType(entityType));
	}

	@Override
	@Transactional
	public List<LayoutSummaryResponseDTO> listSummaries(String entityType) {
		UUID portalId = this.portalContext.requirePortalId();
		requireEntityType(entityType);
		ensureDefault(portalId, entityType);
		List<Layout> layouts = this.layoutRepository.findByPortalIdAndEntityTypeOrderByCreatedAtAscIdAsc(portalId, entityType);
		Graph graph = load(portalId, layouts);
		return layouts.stream().map((layout) -> summary(layout, graph)).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public LayoutEditorResponseDTO editor(UUID layoutId) {
		UUID portalId = this.portalContext.requirePortalId();
		Layout layout = requireLayout(portalId, layoutId);
		return editorOf(portalId, layout);
	}

	@Override
	@Transactional
	public LayoutSummaryResponseDTO create(UUID actorId, String actorName, LayoutCreateDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		String entityType = requireEntityType(request.entityType());
		ensureDefault(portalId, entityType);
		List<Layout> existing = this.layoutRepository.findByPortalIdAndEntityTypeOrderByCreatedAtAscIdAsc(portalId, entityType);
		if (existing.size() >= CustomFieldConstants.LAYOUT_LIMIT) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, CustomFieldConstants.ERROR_LAYOUT_LIMIT,
					CustomFieldConstants.MESSAGE_LAYOUT_LIMIT);
		}
		String name = requireName(request.name(), existing, null);
		Layout source = request.cloneFromId() == null ? null : requireLayout(portalId, request.cloneFromId());
		if (source != null && !source.getEntityType().equals(entityType)) {
			throw notFound();
		}
		Layout layout = new Layout(portalId, entityType, name, clean(request.description()), false, actorId,
				this.clock.instant());
		save(layout);
		if (source == null) {
			this.sectionRepository.save(new LayoutSection(portalId, layout.getId(),
					CustomFieldConstants.DEFAULT_SECTION_NAME, false, 0));
		}
		else {
			copySections(portalId, source, layout);
		}
		if (CustomFieldConstants.ENTITY_TASK.equals(entityType)) {
			if (source == null) {
				this.statusSeeder.seedDefaults(portalId, layout.getId());
			}
			else {
				this.statusSeeder.copy(portalId, source.getId(), layout.getId());
			}
		}
		publish(OutboxEventConstants.EVENT_LAYOUT_CREATED, layout, actorId, actorName, "created");
		return summary(layout, load(portalId, List.of(layout)));
	}

	@Override
	@Transactional
	public LayoutEditorResponseDTO update(UUID actorId, String actorName, UUID layoutId, LayoutUpdateDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		Layout layout = requireLayout(portalId, layoutId);
		List<Layout> all = this.layoutRepository.findByPortalIdAndEntityTypeOrderByCreatedAtAscIdAsc(portalId, layout.getEntityType());
		String name = requireName(request.name(), all, layout.getId());
		List<LayoutSectionUpdateDTO> sections = request.sections() == null ? List.of() : request.sections();
		if (sections.isEmpty()) {
			throw invalid(CustomFieldConstants.DETAIL_SECTIONS, CustomFieldConstants.MESSAGE_SECTIONS_REQUIRED);
		}
		Map<String, CustomField> fieldsByKey = this.fieldRepository
			.findByPortalIdAndEntityTypeAndStatusInOrderBySortOrderAscCreatedAtAsc(portalId, layout.getEntityType(),
					EnumSet.of(FieldStatus.ACTIVE, FieldStatus.INACTIVE))
			.stream()
			.collect(Collectors.toMap(CustomField::getApiName, Function.identity()));
		Map<UUID, List<CustomFieldOption>> options = optionsByField(portalId, fieldsByKey.values());
		Set<String> sectionNames = new LinkedHashSet<>();
		Set<String> placed = new LinkedHashSet<>();
		Map<String, String> defaults = new HashMap<>();
		for (LayoutSectionUpdateDTO section : sections) {
			String sectionName = section.name().trim();
			if (sectionName.isEmpty()) {
				throw invalid(CustomFieldConstants.DETAIL_SECTIONS, CustomFieldConstants.MESSAGE_SECTION_NAME_REQUIRED);
			}
			if (!sectionNames.add(sectionName.toLowerCase())) {
				throw invalid(CustomFieldConstants.DETAIL_SECTIONS, "Two sections are called \"" + sectionName + "\".");
			}
			for (LayoutFieldUpdateDTO placement : section.fields() == null ? List.<LayoutFieldUpdateDTO>of() : section.fields()) {
				CustomField field = fieldsByKey.get(placement.apiName());
				if (field == null) {
					throw invalid(CustomFieldConstants.DETAIL_SECTIONS, "The field " + placement.apiName() + " does not exist.");
				}
				if (!placed.add(placement.apiName())) {
					throw invalid(CustomFieldConstants.DETAIL_SECTIONS, "A field can be placed only once in a layout.");
				}
				try {
					defaults.put(placement.apiName(),
							this.validator.checkPlacementDefault(field, options.getOrDefault(field.getId(), List.of()), placement.defaultValue()));
				}
				catch (ValueProblem problem) {
					throw invalid(CustomFieldConstants.DETAIL_SECTIONS, field.getLabel() + ": " + problem.getMessage());
				}
			}
		}
		Instant now = this.clock.instant();
		// Replace the sections and placements in one go; flush the deletes first so the unique names can be reused.
		this.placementRepository.deleteByPortalIdAndLayoutId(portalId, layout.getId());
		this.sectionRepository.deleteByPortalIdAndLayoutId(portalId, layout.getId());
		this.layoutRepository.flush();
		layout.edit(name, clean(request.description()), actorId, now);
		int sectionOrder = 0;
		for (LayoutSectionUpdateDTO section : sections) {
			LayoutSection saved = this.sectionRepository.save(new LayoutSection(portalId, layout.getId(),
					section.name().trim(), section.collapsible(), sectionOrder++));
			int position = 0;
			for (LayoutFieldUpdateDTO placement : section.fields() == null ? List.<LayoutFieldUpdateDTO>of() : section.fields()) {
				this.placementRepository.save(new LayoutField(portalId, layout.getId(), saved.getId(),
						fieldsByKey.get(placement.apiName()).getId(), position++, placement.mandatory(),
						placement.readOnly(), defaults.get(placement.apiName()), placement.wide()));
			}
		}
		this.layoutRepository.flush();
		publish(OutboxEventConstants.EVENT_LAYOUT_UPDATED, layout, actorId, actorName, "saved");
		return editorOf(portalId, layout);
	}

	@Override
	@Transactional
	public LayoutSummaryResponseDTO makeDefault(UUID actorId, String actorName, UUID layoutId) {
		UUID portalId = this.portalContext.requirePortalId();
		Layout layout = requireLayout(portalId, layoutId);
		if (!layout.isActive()) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, CustomFieldConstants.ERROR_LAYOUT_INVALID,
					CustomFieldConstants.MESSAGE_LAYOUT_ACTIVATE_FIRST);
		}
		if (!layout.isDefaultLayout()) {
			Instant now = this.clock.instant();
			this.layoutRepository.findByPortalIdAndEntityTypeAndDefaultLayoutTrue(portalId, layout.getEntityType())
				.ifPresent((current) -> current.changeDefault(false, actorId, now));
			this.layoutRepository.flush();
			layout.changeDefault(true, actorId, now);
			this.layoutRepository.flush();
			publish(OutboxEventConstants.EVENT_LAYOUT_UPDATED, layout, actorId, actorName, "default");
		}
		return summary(layout, load(portalId, List.of(layout)));
	}

	@Override
	@Transactional
	public LayoutSummaryResponseDTO activate(UUID actorId, String actorName, UUID layoutId) {
		UUID portalId = this.portalContext.requirePortalId();
		Layout layout = requireLayout(portalId, layoutId);
		if (!layout.isActive()) {
			layout.changeActive(true, actorId, this.clock.instant());
			publish(OutboxEventConstants.EVENT_LAYOUT_UPDATED, layout, actorId, actorName, "activated");
		}
		return summary(layout, load(portalId, List.of(layout)));
	}

	@Override
	@Transactional
	public LayoutSummaryResponseDTO deactivate(UUID actorId, String actorName, UUID layoutId) {
		UUID portalId = this.portalContext.requirePortalId();
		Layout layout = requireLayout(portalId, layoutId);
		if (layout.isDefaultLayout()) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, CustomFieldConstants.ERROR_LAYOUT_INVALID,
					CustomFieldConstants.MESSAGE_LAYOUT_DEFAULT_INACTIVE);
		}
		if (layout.isActive()) {
			// Records already on it keep it; it just stops being offered.
			layout.changeActive(false, actorId, this.clock.instant());
			publish(OutboxEventConstants.EVENT_LAYOUT_UPDATED, layout, actorId, actorName, "deactivated");
		}
		return summary(layout, load(portalId, List.of(layout)));
	}

	/* ---------- inside the module: fields and the validator ---------- */

	@Override
	@Transactional
	public void placeNewField(UUID portalId, CustomField field) {
		Optional<Layout> current = this.layoutRepository.findByPortalIdAndEntityTypeAndDefaultLayoutTrue(portalId,
				field.getEntityType());
		if (current.isEmpty()) {
			createDefault(portalId, field.getEntityType()); // the new field is already saved, so it is placed with the rest
			return;
		}
		Layout layout = current.get();
		List<LayoutSection> sections = this.sectionRepository.findByPortalIdAndLayoutIdInOrderBySortOrderAsc(portalId,
				List.of(layout.getId()));
		LayoutSection last = sections.get(sections.size() - 1);
		int position = (int) this.placementRepository.countByPortalIdAndLayoutId(portalId, layout.getId());
		this.placementRepository.save(new LayoutField(portalId, layout.getId(), last.getId(), field.getId(), position,
				null, false, null, null));
	}

	@Override
	@Transactional
	public void removePlacements(UUID portalId, UUID fieldId) {
		this.placementRepository.deleteByPortalIdAndFieldId(portalId, fieldId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<FieldRule> rulesFor(UUID portalId, String entityType, UUID layoutId) {
		Layout layout = requireLayout(portalId, layoutId);
		if (!layout.getEntityType().equals(entityType)) {
			throw notFound();
		}
		Graph graph = load(portalId, List.of(layout));
		List<FieldRule> rules = new ArrayList<>();
		for (LayoutSection section : graph.sections().getOrDefault(layout.getId(), List.of())) {
			for (LayoutField placement : placementsOf(layout, section, graph)) {
				CustomField field = graph.fields().get(placement.getFieldId());
				if (field != null && field.getStatus() == FieldStatus.ACTIVE) {
					rules.add(new FieldRule(field,
							placement.getMandatory() != null ? placement.getMandatory() : field.isMandatory(),
							placement.isReadOnly(), placement.getDefaultValue()));
				}
			}
		}
		return rules;
	}

	/* ---------- helpers ---------- */

	/** The module's default layout; the first time it is needed it is created, holding every field in one section. */
	private Layout ensureDefault(UUID portalId, String entityType) {
		return this.layoutRepository.findByPortalIdAndEntityTypeAndDefaultLayoutTrue(portalId, entityType)
			.orElseGet(() -> createDefault(portalId, entityType));
	}

	private Layout createDefault(UUID portalId, String entityType) {
		Layout layout = new Layout(portalId, entityType, CustomFieldConstants.DEFAULT_LAYOUT_NAME,
				CustomFieldConstants.DEFAULT_LAYOUT_DESCRIPTION, true, CustomFieldConstants.SYSTEM_ACTOR,
				this.clock.instant());
		save(layout);
		LayoutSection section = this.sectionRepository.save(new LayoutSection(portalId, layout.getId(),
				CustomFieldConstants.DEFAULT_SECTION_NAME, false, 0));
		int position = 0;
		for (CustomField field : this.fieldRepository.findByPortalIdAndEntityTypeAndStatusInOrderBySortOrderAscCreatedAtAsc(
				portalId, entityType, EnumSet.of(FieldStatus.ACTIVE, FieldStatus.INACTIVE))) {
			this.placementRepository.save(new LayoutField(portalId, layout.getId(), section.getId(), field.getId(),
					position++, null, false, null, null));
		}
		if (CustomFieldConstants.ENTITY_TASK.equals(entityType)) {
			this.statusSeeder.seedDefaults(portalId, layout.getId());
		}
		publish(OutboxEventConstants.EVENT_LAYOUT_CREATED, layout, CustomFieldConstants.SYSTEM_ACTOR, "System", "created");
		return layout;
	}

	private void copySections(UUID portalId, Layout source, Layout target) {
		Graph graph = load(portalId, List.of(source));
		for (LayoutSection section : graph.sections().getOrDefault(source.getId(), List.of())) {
			LayoutSection copy = this.sectionRepository.save(new LayoutSection(portalId, target.getId(),
					section.getName(), section.isCollapsible(), section.getSortOrder()));
			for (LayoutField placement : placementsOf(source, section, graph)) {
				this.placementRepository.save(new LayoutField(portalId, target.getId(), copy.getId(),
						placement.getFieldId(), placement.getSortOrder(), placement.getMandatory(),
						placement.isReadOnly(), placement.getDefaultValue(), placement.getWide()));
			}
		}
	}

	private Graph load(UUID portalId, List<Layout> layouts) {
		if (layouts.isEmpty()) {
			return new Graph(Map.of(), Map.of(), Map.of(), Map.of());
		}
		List<UUID> ids = layouts.stream().map(Layout::getId).toList();
		Map<UUID, List<LayoutSection>> sections = this.sectionRepository
			.findByPortalIdAndLayoutIdInOrderBySortOrderAsc(portalId, ids)
			.stream()
			.collect(Collectors.groupingBy(LayoutSection::getLayoutId, LinkedHashMap::new, Collectors.toList()));
		List<LayoutField> all = this.placementRepository.findByPortalIdAndLayoutIdInOrderBySortOrderAsc(portalId, ids);
		Map<UUID, List<LayoutField>> placements = all.stream().collect(Collectors.groupingBy(LayoutField::getLayoutId));
		List<UUID> fieldIds = all.stream().map(LayoutField::getFieldId).distinct().toList();
		Map<UUID, CustomField> fields = this.fieldRepository.findAllById(fieldIds)
			.stream()
			.filter((field) -> field.getPortalId().equals(portalId))
			.collect(Collectors.toMap(CustomField::getId, Function.identity()));
		Map<UUID, List<CustomFieldOption>> options = fieldIds.isEmpty() ? Map.of()
				: this.optionRepository.findByPortalIdAndFieldIdInOrderBySortOrderAsc(portalId, fieldIds)
					.stream()
					.collect(Collectors.groupingBy(CustomFieldOption::getFieldId));
		return new Graph(sections, placements, fields, options);
	}

	private Map<UUID, List<CustomFieldOption>> optionsByField(UUID portalId, Collection<CustomField> fields) {
		if (fields.isEmpty()) {
			return Map.of();
		}
		return this.optionRepository
			.findByPortalIdAndFieldIdInOrderBySortOrderAsc(portalId, fields.stream().map(CustomField::getId).toList())
			.stream()
			.collect(Collectors.groupingBy(CustomFieldOption::getFieldId));
	}

	private List<LayoutField> placementsOf(Layout layout, LayoutSection section, Graph graph) {
		return graph.placements()
			.getOrDefault(layout.getId(), List.of())
			.stream()
			.filter((placement) -> placement.getSectionId().equals(section.getId()))
			.toList();
	}

	/** Sections in order, each with its active fields and the layout's rules applied. */
	private LayoutResponseDTO resolve(Layout layout, Graph graph) {
		List<LayoutSectionResponseDTO> sections = new ArrayList<>();
		for (LayoutSection section : graph.sections().getOrDefault(layout.getId(), List.of())) {
			List<LayoutFieldResponseDTO> fields = new ArrayList<>();
			for (LayoutField placement : placementsOf(layout, section, graph)) {
				CustomField field = graph.fields().get(placement.getFieldId());
				if (field != null && field.getStatus() == FieldStatus.ACTIVE) {
					fields.add(this.layoutMapper.toFieldResponse(field, placement,
							graph.options().getOrDefault(field.getId(), List.of())));
				}
			}
			sections.add(new LayoutSectionResponseDTO(section.getId(), section.getName(), section.isCollapsible(), fields));
		}
		return new LayoutResponseDTO(layout.getId(), layout.getEntityType(), layout.getName(), layout.getDescription(),
				layout.isDefaultLayout(), layout.isActive(), sections, statusesOf(layout));
	}

	private LayoutSummaryResponseDTO summary(Layout layout, Graph graph) {
		List<LayoutSection> sections = graph.sections().getOrDefault(layout.getId(), List.of());
		long fields = graph.placements()
			.getOrDefault(layout.getId(), List.of())
			.stream()
			.filter((placement) -> {
				CustomField field = graph.fields().get(placement.getFieldId());
				return field != null && field.getStatus() == FieldStatus.ACTIVE;
			})
			.count();
		return new LayoutSummaryResponseDTO(layout.getId(), layout.getEntityType(), layout.getName(),
				layout.getDescription(), layout.isDefaultLayout(), layout.isActive(), sections.size(), (int) fields,
				usage(layout));
	}

	private long usage(Layout layout) {
		return this.holders.stream()
			.filter((holder) -> holder.entityType().equals(layout.getEntityType()))
			.mapToLong((holder) -> holder.countUsingLayout(layout.getPortalId(), layout.getId()))
			.sum();
	}

	private LayoutEditorResponseDTO editorOf(UUID portalId, Layout layout) {
		Graph graph = load(portalId, List.of(layout));
		List<LayoutSectionEditorResponseDTO> sections = new ArrayList<>();
		for (LayoutSection section : graph.sections().getOrDefault(layout.getId(), List.of())) {
			List<LayoutPlacementResponseDTO> placements = new ArrayList<>();
			for (LayoutField placement : placementsOf(layout, section, graph)) {
				CustomField field = graph.fields().get(placement.getFieldId());
				if (field != null && field.getStatus() != FieldStatus.DELETED) {
					placements.add(this.layoutMapper.toPlacement(field, placement));
				}
			}
			sections.add(new LayoutSectionEditorResponseDTO(section.getId(), section.getName(), section.isCollapsible(), placements));
		}
		List<CustomField> live = this.fieldRepository.findByPortalIdAndEntityTypeAndStatusInOrderBySortOrderAscCreatedAtAsc(
				portalId, layout.getEntityType(), EnumSet.of(FieldStatus.ACTIVE, FieldStatus.INACTIVE));
		Map<UUID, List<CustomFieldOption>> options = optionsByField(portalId, live);
		return new LayoutEditorResponseDTO(layout.getId(), layout.getEntityType(), layout.getName(),
				layout.getDescription(), layout.isDefaultLayout(), layout.isActive(), usage(layout), sections,
				live.stream().map((field) -> this.fieldMapper.toResponse(field, options.getOrDefault(field.getId(), List.of()))).toList(),
				statusesOf(layout));
	}

	/** Only TASK layouts have statuses. */
	private List<LayoutStatusResponseDTO> statusesOf(Layout layout) {
		if (!CustomFieldConstants.ENTITY_TASK.equals(layout.getEntityType())) {
			return List.of();
		}
		return this.statusRepository
			.findByPortalIdAndLayoutIdOrderBySortOrderAscIdAsc(layout.getPortalId(), layout.getId())
			.stream()
			.map(this.layoutMapper::toStatus)
			.toList();
	}

	/** Another portal's layout is "not found" too. */
	private Layout requireLayout(UUID portalId, UUID layoutId) {
		return this.layoutRepository.findByIdAndPortalId(layoutId, portalId).orElseThrow(this::notFound);
	}

	private String requireName(String raw, List<Layout> existing, UUID selfId) {
		String name = raw == null ? "" : raw.trim();
		if (name.isEmpty()) {
			throw invalid(CustomFieldConstants.DETAIL_NAME, CustomFieldConstants.MESSAGE_LAYOUT_NAME_REQUIRED);
		}
		if (existing.stream().anyMatch((layout) -> !layout.getId().equals(selfId) && layout.getName().equalsIgnoreCase(name))) {
			throw invalid(CustomFieldConstants.DETAIL_NAME, CustomFieldConstants.MESSAGE_LAYOUT_NAME_TAKEN);
		}
		return name;
	}

	private static String requireEntityType(String entityType) {
		if (entityType == null || !CustomFieldConstants.ENTITY_TYPES.contains(entityType)) {
			throw new AppException(HttpStatus.BAD_REQUEST, CustomFieldConstants.ERROR_ENTITY_UNSUPPORTED,
					CustomFieldConstants.MESSAGE_ENTITY_UNSUPPORTED);
		}
		return entityType;
	}

	private void save(Layout layout) {
		try {
			this.layoutRepository.saveAndFlush(layout);
		}
		catch (DataIntegrityViolationException ex) {
			throw new AppException(HttpStatus.CONFLICT, CustomFieldConstants.ERROR_LAYOUT_INVALID,
					"A layout was just created at the same time. Try again.");
		}
	}

	private static AppException invalid(String detail, String message) {
		Map<String, Object> details = new HashMap<>();
		details.put(detail, message);
		return new AppException(HttpStatus.BAD_REQUEST, AppConstants.ERROR_VALIDATION_FAILED,
				AppConstants.MESSAGE_VALIDATION_FAILED, details);
	}

	private AppException notFound() {
		return new AppException(HttpStatus.NOT_FOUND, CustomFieldConstants.ERROR_LAYOUT_NOT_FOUND,
				CustomFieldConstants.MESSAGE_LAYOUT_NOT_FOUND);
	}

	private static String clean(String text) {
		return text == null || text.isBlank() ? null : text.trim();
	}

	/** Same transaction as the change; the relay sends it to Kafka afterwards. Ids only, no personal data. */
	private void publish(String eventType, Layout layout, UUID actorId, String actorName, String change) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("portalId", layout.getPortalId());
		payload.put("layoutId", layout.getId());
		payload.put("entityType", layout.getEntityType());
		payload.put("change", change);
		this.outboxWriteService.publish(new OutboxEventCreateDTO(layout.getPortalId(),
				OutboxEventConstants.AGGREGATE_LAYOUT, layout.getId(), eventType, actorId, actorName, payload));
	}

}

package com.projecty.work.modules.project.internal.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.exception.AppException;
import com.projecty.work.common.service.RequestPortalContext;
import com.projecty.work.modules.access.api.MemberAccessLookUpService;
import com.projecty.work.modules.access.api.dto.MemberAccessResponseDTO;
import com.projecty.work.modules.customfield.api.CustomFieldLookUpService;
import com.projecty.work.modules.customfield.api.CustomFieldWriteService;
import com.projecty.work.modules.customfield.api.dto.LayoutResponseDTO;
import com.projecty.work.modules.customfield.api.dto.LayoutFieldResponseDTO;
import com.projecty.work.modules.customfield.api.LayoutLookUpService;
import com.projecty.work.modules.customfield.api.dto.CustomFieldResponseDTO;
import com.projecty.work.modules.outbox.api.OutboxWriteService;
import com.projecty.work.modules.outbox.api.dto.OutboxEventCreateDTO;
import com.projecty.work.modules.shared.constants.OutboxEventConstants;
import com.projecty.work.modules.shared.helper.CacheKeyHelper;
import com.projecty.work.modules.shared.constants.PermissionConstants;
import com.projecty.work.modules.project.api.ProjectCacheService;
import com.projecty.work.modules.project.api.dto.ProjectSummaryDTO;
import com.projecty.work.modules.project.internal.constants.ProjectConstants;
import com.projecty.work.modules.project.internal.dto.KeyAvailabilityResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectActivityResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectCreateDTO;
import com.projecty.work.modules.project.internal.dto.ProjectDetailResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectIdResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectMemberAddDTO;
import com.projecty.work.modules.project.internal.dto.ProjectMemberResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectStatusResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectUpdateDTO;
import com.projecty.work.modules.project.internal.entity.Project;
import com.projecty.work.modules.project.internal.entity.ProjectActivity;
import com.projecty.work.modules.project.internal.entity.ProjectMember;
import com.projecty.work.modules.project.internal.entity.ProjectRole;
import com.projecty.work.modules.project.internal.entity.ProjectVisibility;
import com.projecty.work.modules.project.internal.helper.ProjectResponseCache;
import com.projecty.work.modules.project.internal.mapper.ProjectMapper;
import com.projecty.work.modules.project.internal.repository.ProjectActivityRepository;
import com.projecty.work.modules.project.internal.repository.ProjectMemberRepository;
import com.projecty.work.modules.project.internal.repository.ProjectRepository;
import com.projecty.work.modules.project.internal.service.ProjectService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectServiceImpl implements ProjectService {

	private final ProjectRepository projectRepository;

	private final ProjectMemberRepository memberRepository;

	private final ProjectActivityRepository activityRepository;

	private final MemberAccessLookUpService accessLookUpService;

	private final CustomFieldLookUpService customFieldLookUpService;

	private final CustomFieldWriteService customFieldWriteService;

	private final LayoutLookUpService layoutLookUpService;

	private final OutboxWriteService outboxWriteService;

	private final RequestPortalContext portalContext;

	private final ProjectMapper mapper;

	private final ProjectCacheService projectCache;

	private final ProjectResponseCache responseCache;

	private final Clock clock;

	public ProjectServiceImpl(ProjectRepository projectRepository, ProjectMemberRepository memberRepository,
			ProjectActivityRepository activityRepository, MemberAccessLookUpService accessLookUpService,
			CustomFieldLookUpService customFieldLookUpService, CustomFieldWriteService customFieldWriteService,
			LayoutLookUpService layoutLookUpService, OutboxWriteService outboxWriteService, RequestPortalContext portalContext, ProjectMapper mapper,
			ProjectCacheService projectCache, ProjectResponseCache responseCache, Clock clock) {
		this.projectRepository = projectRepository;
		this.memberRepository = memberRepository;
		this.activityRepository = activityRepository;
		this.accessLookUpService = accessLookUpService;
		this.customFieldLookUpService = customFieldLookUpService;
		this.customFieldWriteService = customFieldWriteService;
		this.layoutLookUpService = layoutLookUpService;
		this.outboxWriteService = outboxWriteService;
		this.portalContext = portalContext;
		this.mapper = mapper;
		this.projectCache = projectCache;
		this.responseCache = responseCache;
		this.clock = clock;
	}

	/** Who is asking: their profile and permissions in this portal, from Work's copy of Platform's member events. */
	private record Caller(UUID userId, boolean admin, boolean client, Set<String> permissions) {
	}

	/** A project the caller may see, with the caller's role in it (null if not a member). */
	private record Found(Project project, ProjectRole role) {
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProjectResponseDTO> list(UUID actorId) {
		UUID portalId = this.portalContext.requirePortalId();
		Caller caller = caller(portalId, actorId);
		return this.responseCache.list(portalId, actorId, fingerprint(caller), () -> {
			Map<UUID, ProjectRole> roles = rolesOf(portalId, actorId);
			Map<UUID, Integer> counts = memberCounts(portalId);
			Set<String> activeKeys = activeFieldKeys(portalId);
			return this.projectRepository.findByPortalIdAndDeletedAtIsNull(portalId)
				.stream()
				.filter((project) -> canSee(project, caller, roles.get(project.getId())))
				.sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
				.map((project) -> view(project, caller, roles, counts, activeKeys))
				.toList();
		});
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProjectResponseDTO> listTrash(UUID actorId) {
		UUID portalId = this.portalContext.requirePortalId();
		Caller caller = caller(portalId, actorId);
		Map<UUID, ProjectRole> roles = rolesOf(portalId, actorId);
		Map<UUID, Integer> counts = memberCounts(portalId);
		Set<String> activeKeys = activeFieldKeys(portalId);
		return this.projectRepository.findByPortalIdAndDeletedAtIsNotNull(portalId)
			.stream()
			.filter((project) -> canDelete(project, caller))
			.sorted(Comparator.comparing(Project::getDeletedAt).reversed())
			.map((project) -> view(project, caller, roles, counts, activeKeys))
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ProjectDetailResponseDTO get(UUID actorId, UUID projectId) {
		UUID portalId = this.portalContext.requirePortalId();
		Caller caller = caller(portalId, actorId);
		return this.responseCache.detail(portalId, projectId, actorId, fingerprint(caller),
				() -> loadDetail(portalId, projectId, caller));
	}

	private ProjectDetailResponseDTO loadDetail(UUID portalId, UUID projectId, Caller caller) {
		Found found = findVisible(portalId, projectId, caller);
		List<ProjectMember> members = this.memberRepository.findByPortalIdAndProjectId(portalId, projectId);
		List<ProjectActivityResponseDTO> activity = this.activityRepository
			.findByPortalIdAndProjectIdOrderByCreatedAtDesc(portalId, projectId,
					PageRequest.of(0, ProjectConstants.ACTIVITY_LIMIT))
			.stream()
			.map(this.mapper::toActivity)
			.toList();
		LayoutResponseDTO layout = this.layoutLookUpService.getLayout(portalId, found.project().getLayoutId());
		Set<String> placed = layout.sections()
			.stream()
			.flatMap((section) -> section.fields().stream())
			.map(LayoutFieldResponseDTO::apiName)
			.collect(Collectors.toSet());
		Set<String> hidden = activeFieldKeys(portalId);
		hidden.removeAll(placed);
		return new ProjectDetailResponseDTO(
				this.mapper.toResponse(found.project(), members.size(), found.role() != null,
						canEdit(found.project(), caller, found.role()), canDelete(found.project(), caller),
						activeValues(found.project(), placed)),
				memberResponses(found.project(), members), activity, layout, activeValues(found.project(), hidden));
	}

	@Override
	@Transactional
	public ProjectIdResponseDTO create(UUID actorId, String actorName, ProjectCreateDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		caller(portalId, actorId);
		requireDates(request.startDate(), request.endDate());
		UUID ownerId = request.ownerUserId() != null ? request.ownerUserId() : actorId;
		requireActivePerson(portalId, ownerId);
		if (this.projectRepository.existsByPortalIdAndProjectKey(portalId, request.key())) {
			throw keyTaken();
		}
		UUID layoutId = request.layoutId() != null ? request.layoutId()
				: this.layoutLookUpService.getDefaultLayoutId(portalId, ProjectConstants.CUSTOM_FIELD_ENTITY);
		requireActiveLayout(portalId, layoutId, ProjectConstants.CUSTOM_FIELD_ENTITY);
		Map<String, Object> customValues = this.customFieldLookUpService.validate(portalId,
				ProjectConstants.CUSTOM_FIELD_ENTITY, layoutId,
				request.customValues() == null ? Map.of() : request.customValues(), Map.of(), true);
		Instant now = this.clock.instant();
		Project project = new Project(portalId, request.key(), request.name().trim(), clean(request.description()),
				ownerId, ProjectConstants.STATUS_DEFAULT, request.startDate(), request.endDate(),
				request.visibility(), layoutId, now);
		project.changeCustomValues(customValues, now);
		try {
			this.projectRepository.saveAndFlush(project);
		}
		catch (DataIntegrityViolationException ex) {
			throw keyTaken();
		}
		this.customFieldWriteService.reserveUniqueValues(portalId, ProjectConstants.CUSTOM_FIELD_ENTITY,
				project.getId(), Map.of(), customValues);
		// PRJ-BR-05: the owner is always a member with the Manager role.
		this.memberRepository.save(new ProjectMember(portalId, project.getId(), ownerId, ProjectRole.MANAGER, now));
		record(project, ProjectConstants.ACTION_CREATED, actorId, actorName, null, "Created project");
		publish(OutboxEventConstants.EVENT_PROJECT_CREATED, project, actorId, actorName,
				Map.of("ownerUserId", ownerId));
		return new ProjectIdResponseDTO(project.getId());
	}

	@Override
	@Transactional
	public ProjectResponseDTO update(UUID actorId, String actorName, UUID projectId, ProjectUpdateDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		Caller caller = caller(portalId, actorId);
		Found found = findVisible(portalId, projectId, caller, true);
		Project project = found.project();
		requireCanEdit(project, caller, found.role());
		// Only what the request carries changes; everything else keeps its stored value.
		String name = project.getName();
		if (request.isNameSet()) {
			name = clean(request.getName());
			if (name.isEmpty()) {
				throw invalid("name", ProjectConstants.MESSAGE_NAME_REQUIRED);
			}
			if (name.length() > ProjectConstants.NAME_MAX_LENGTH) {
				throw invalid("name", ProjectConstants.MESSAGE_NAME_TOO_LONG);
			}
		}
		String description = project.getDescription();
		if (request.isDescriptionSet()) {
			description = clean(request.getDescription());
			if (description.length() > ProjectConstants.DESCRIPTION_MAX_LENGTH) {
				throw invalid("description", ProjectConstants.MESSAGE_DESCRIPTION_TOO_LONG);
			}
		}
		ProjectVisibility visibility = project.getVisibility();
		if (request.isVisibilitySet()) {
			if (request.getVisibility() == null) {
				throw invalid("visibility", ProjectConstants.MESSAGE_VISIBILITY_REQUIRED);
			}
			visibility = request.getVisibility();
		}
		LocalDate startDate = request.isStartDateSet() ? request.getStartDate() : project.getStartDate();
		LocalDate endDate = request.isEndDateSet() ? request.getEndDate() : project.getEndDate();
		requireDates(startDate, endDate);
		UUID ownerId = request.isOwnerUserIdSet() && request.getOwnerUserId() != null ? request.getOwnerUserId()
				: project.getOwnerUserId();
		boolean ownerChanged = !ownerId.equals(project.getOwnerUserId());
		if (ownerChanged) {
			requireActivePerson(portalId, ownerId);
		}
		List<String> changes = new ArrayList<>();
		addIf(changes, "name", !project.getName().equals(name));
		addIf(changes, "description", !Objects.equals(project.getDescription(), description));
		addIf(changes, "owner", ownerChanged);
		addIf(changes, "start date", !Objects.equals(project.getStartDate(), startDate));
		addIf(changes, "end date", !Objects.equals(project.getEndDate(), endDate));
		addIf(changes, "visibility", project.getVisibility() != visibility);
		boolean coreChanged = !changes.isEmpty();
		// The row is locked, so what we read here is what we replace: concurrent edits queue instead of overwriting.
		Map<String, Object> before = new HashMap<>(project.getCustomValues());
		Map<String, Object> after = before;
		if (request.isCustomValuesSet() && request.getCustomValues() != null) {
			after = this.customFieldLookUpService.validate(portalId, ProjectConstants.CUSTOM_FIELD_ENTITY,
					project.getLayoutId(), request.getCustomValues(), before, false);
			this.customFieldLookUpService.changedKeys(before, after)
				.forEach((key) -> changes.add("custom field " + key));
		}
		Instant now = this.clock.instant();
		if (coreChanged) {
			project.edit(name, description, ownerId, startDate, endDate, visibility, now);
		}
		if (after != before) {
			project.changeCustomValues(after, now);
			this.customFieldWriteService.reserveUniqueValues(portalId, ProjectConstants.CUSTOM_FIELD_ENTITY,
					project.getId(), before, after);
		}
		if (ownerChanged) {
			// PRJ-BR-05: the new owner is a Manager member; the old owner stays a member.
			ensureManager(portalId, project.getId(), ownerId, now);
		}
		if (!changes.isEmpty()) {
			String detail = "Changed " + String.join(", ", changes);
			record(project, ProjectConstants.ACTION_UPDATED, actorId, actorName, null, detail);
			publish(OutboxEventConstants.EVENT_PROJECT_UPDATED, project, actorId, actorName,
					Map.of("changes", changes));
		}
		return viewOne(project, caller, found.role(), portalId);
	}

	@Override
	@Transactional
	public ProjectDetailResponseDTO changeLayout(UUID actorId, String actorName, UUID projectId, UUID layoutId) {
		UUID portalId = this.portalContext.requirePortalId();
		Caller caller = caller(portalId, actorId);
		Found found = findVisible(portalId, projectId, caller, true);
		Project project = found.project();
		requireCanEdit(project, caller, found.role());
		if (!project.getLayoutId().equals(layoutId)) {
			LayoutResponseDTO target = requireActiveLayout(portalId, layoutId, ProjectConstants.CUSTOM_FIELD_ENTITY);
			LayoutResponseDTO current = this.layoutLookUpService.getLayout(portalId, project.getLayoutId());
			project.changeLayout(layoutId, this.clock.instant());
			record(project, ProjectConstants.ACTION_UPDATED, actorId, actorName, null,
					"Changed layout (" + current.name() + " → " + target.name() + ")");
			publish(OutboxEventConstants.EVENT_PROJECT_LAYOUT_CHANGED, project, actorId, actorName,
					Map.of("fromLayoutId", current.id(), "toLayoutId", target.id()));
		}
		return get(actorId, projectId);
	}

	@Override
	@Transactional
	public ProjectDetailResponseDTO changeTaskLayout(UUID actorId, String actorName, UUID projectId, UUID layoutId) {
		UUID portalId = this.portalContext.requirePortalId();
		Caller caller = caller(portalId, actorId);
		Found found = findVisible(portalId, projectId, caller, true);
		Project project = found.project();
		requireCanEdit(project, caller, found.role());
		LayoutResponseDTO target = requireActiveLayout(portalId, layoutId, ProjectConstants.TASK_LAYOUT_ENTITY);
		UUID currentId = project.getTaskLayoutId() != null ? project.getTaskLayoutId()
				: this.layoutLookUpService.getDefaultLayoutId(portalId, ProjectConstants.TASK_LAYOUT_ENTITY);
		if (!currentId.equals(layoutId)) {
			LayoutResponseDTO current = this.layoutLookUpService.getLayout(portalId, currentId);
			project.changeTaskLayout(layoutId, this.clock.instant());
			record(project, ProjectConstants.ACTION_UPDATED, actorId, actorName, null,
					"Changed task layout (" + current.name() + " → " + target.name() + ")");
			publish(OutboxEventConstants.EVENT_PROJECT_LAYOUT_CHANGED, project, actorId, actorName,
					Map.of("entityType", ProjectConstants.TASK_LAYOUT_ENTITY, "fromLayoutId", current.id(),
							"toLayoutId", target.id()));
		}
		return get(actorId, projectId);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ProjectSummaryDTO> findVisibleProject(UUID portalId, UUID projectId, UUID userId) {
		Caller caller = caller(portalId, userId);
		return this.responseCache.visible(portalId, projectId, userId, fingerprint(caller),
				() -> this.projectRepository.findByIdAndPortalId(projectId, portalId)
					.filter((project) -> !project.isTrashed())
					.filter((project) -> canSee(project, caller, roleOf(portalId, projectId, userId)))
					.map((project) -> new ProjectSummaryDTO(project.getId(), project.getProjectKey(),
							project.getName(), project.getTaskLayoutId())));
	}

	@Override
	@Transactional
	public ProjectResponseDTO changeStatus(UUID actorId, String actorName, UUID projectId, String status) {
		UUID portalId = this.portalContext.requirePortalId();
		Caller caller = caller(portalId, actorId);
		Found found = findVisible(portalId, projectId, caller);
		Project project = found.project();
		requireCanEdit(project, caller, found.role());
		if (ProjectConstants.STATUSES.stream().noneMatch((s) -> s.name().equals(status))) {
			throw new AppException(HttpStatus.BAD_REQUEST, ProjectConstants.ERROR_STATUS_INVALID,
					ProjectConstants.MESSAGE_STATUS_INVALID);
		}
		if (!project.getStatus().equals(status)) {
			String from = project.getStatus();
			project.changeStatus(status, this.clock.instant());
			record(project, ProjectConstants.ACTION_STATUS_CHANGED, actorId, actorName, null,
					"Status: " + from + " → " + status);
			publish(OutboxEventConstants.EVENT_PROJECT_STATUS_CHANGED, project, actorId, actorName,
					Map.of("fromStatus", from, "toStatus", status));
		}
		return viewOne(project, caller, found.role(), portalId);
	}

	@Override
	@Transactional
	public void trash(UUID actorId, String actorName, UUID projectId) {
		UUID portalId = this.portalContext.requirePortalId();
		Caller caller = caller(portalId, actorId);
		Project project = findVisible(portalId, projectId, caller).project();
		if (!canDelete(project, caller)) {
			throw forbidden(ProjectConstants.MESSAGE_FORBIDDEN_DELETE);
		}
		project.trash(actorId, this.clock.instant());
		record(project, ProjectConstants.ACTION_TRASHED, actorId, actorName, null, "Moved to trash");
		publish(OutboxEventConstants.EVENT_PROJECT_TRASHED, project, actorId, actorName, Map.of());
	}

	@Override
	@Transactional
	public ProjectResponseDTO restore(UUID actorId, String actorName, UUID projectId) {
		UUID portalId = this.portalContext.requirePortalId();
		Caller caller = caller(portalId, actorId);
		Project project = this.projectRepository.findByIdAndPortalId(projectId, portalId)
			.filter(Project::isTrashed)
			.filter((trashed) -> canDelete(trashed, caller))
			.orElseThrow(this::notFound);
		project.restore(this.clock.instant());
		record(project, ProjectConstants.ACTION_RESTORED, actorId, actorName, null, "Restored from trash");
		publish(OutboxEventConstants.EVENT_PROJECT_RESTORED, project, actorId, actorName, Map.of());
		return viewOne(project, caller, roleOf(portalId, projectId, actorId), portalId);
	}

	@Override
	@Transactional
	public List<ProjectMemberResponseDTO> addMember(UUID actorId, String actorName, UUID projectId,
			ProjectMemberAddDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		Caller caller = caller(portalId, actorId);
		Found found = findVisible(portalId, projectId, caller);
		Project project = found.project();
		requireCanEdit(project, caller, found.role());
		requireActivePerson(portalId, request.userId());
		if (roleOf(portalId, projectId, request.userId()) != null) {
			throw new AppException(HttpStatus.CONFLICT, ProjectConstants.ERROR_ALREADY_MEMBER,
					ProjectConstants.MESSAGE_ALREADY_MEMBER);
		}
		ProjectRole role = request.role() != null ? request.role() : ProjectRole.MEMBER;
		this.memberRepository.save(new ProjectMember(portalId, projectId, request.userId(), role, this.clock.instant()));
		record(project, ProjectConstants.ACTION_MEMBER_ADDED, actorId, actorName, request.userId(),
				"Added as " + role.name().toLowerCase());
		publish(OutboxEventConstants.EVENT_PROJECT_MEMBER_ADDED, project, actorId, actorName,
				Map.of("userId", request.userId(), "role", role.name()));
		return memberResponses(project, this.memberRepository.findByPortalIdAndProjectId(portalId, projectId));
	}

	@Override
	@Transactional
	public List<ProjectMemberResponseDTO> removeMember(UUID actorId, String actorName, UUID projectId, UUID userId) {
		UUID portalId = this.portalContext.requirePortalId();
		Caller caller = caller(portalId, actorId);
		Found found = findVisible(portalId, projectId, caller);
		Project project = found.project();
		requireCanEdit(project, caller, found.role());
		if (userId.equals(project.getOwnerUserId())) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, ProjectConstants.ERROR_OWNER_LOCKED,
					ProjectConstants.MESSAGE_OWNER_LOCKED);
		}
		ProjectMember member = this.memberRepository.findByPortalIdAndProjectIdAndUserId(portalId, projectId, userId)
			.orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ProjectConstants.ERROR_MEMBER_NOT_FOUND,
					ProjectConstants.MESSAGE_MEMBER_NOT_FOUND));
		this.memberRepository.delete(member);
		record(project, ProjectConstants.ACTION_MEMBER_REMOVED, actorId, actorName, userId, "Removed from project");
		publish(OutboxEventConstants.EVENT_PROJECT_MEMBER_REMOVED, project, actorId, actorName,
				Map.of("userId", userId));
		return memberResponses(project, this.memberRepository.findByPortalIdAndProjectId(portalId, projectId));
	}

	@Override
	@Transactional(readOnly = true)
	public KeyAvailabilityResponseDTO checkKey(String key) {
		UUID portalId = this.portalContext.requirePortalId();
		boolean available = key != null && key.matches(ProjectConstants.KEY_PATTERN)
				&& !this.projectRepository.existsByPortalIdAndProjectKey(portalId, key);
		return new KeyAvailabilityResponseDTO(available);
	}

	@Override
	public List<ProjectStatusResponseDTO> statuses() {
		return ProjectConstants.STATUSES.stream().map(this.mapper::toStatus).toList();
	}

	/* ---------- who may do what ---------- */

	private Caller caller(UUID portalId, UUID userId) {
		MemberAccessResponseDTO access = this.accessLookUpService.findMemberAccess(portalId, userId)
			.filter(MemberAccessResponseDTO::active)
			.orElseThrow(() -> new AppException(HttpStatus.FORBIDDEN, AppConstants.ERROR_PORTAL_ACCESS_DENIED,
					AppConstants.MESSAGE_PORTAL_ACCESS_DENIED));
		return new Caller(userId, ProjectConstants.PROFILE_ADMIN.equals(access.profileKey()),
				ProjectConstants.PROFILE_CLIENT.equals(access.profileKey()), access.permissions());
	}

	/** What decides how the caller sees things (admin, client, permissions), for cache keys. */
	private static String fingerprint(Caller caller) {
		return CacheKeyHelper.fingerprint(caller.admin(), caller.client(), caller.permissions());
	}

	/** PRJ-BR-06: Admins see all; others see projects they are in, and public ones unless they are Clients. */
	private static boolean canSee(Project project, Caller caller, ProjectRole role) {
		return caller.admin() || role != null
				|| (project.getVisibility() == ProjectVisibility.PUBLIC && !caller.client());
	}

	/** Needs PROJECTS_EDIT and either the Admin profile, being the owner, or the Manager project role. */
	private static boolean canEdit(Project project, Caller caller, ProjectRole role) {
		return caller.permissions().contains(PermissionConstants.PROJECTS_EDIT) && (caller.admin()
				|| project.getOwnerUserId().equals(caller.userId()) || role == ProjectRole.MANAGER);
	}

	/** PRJ-BR-10: only the owner or an Admin, and only with PROJECTS_DELETE. */
	private static boolean canDelete(Project project, Caller caller) {
		return caller.permissions().contains(PermissionConstants.PROJECTS_DELETE)
				&& (caller.admin() || project.getOwnerUserId().equals(caller.userId()));
	}

	private void requireCanEdit(Project project, Caller caller, ProjectRole role) {
		if (!canEdit(project, caller, role)) {
			throw forbidden(ProjectConstants.MESSAGE_FORBIDDEN_EDIT);
		}
	}

	/** Another portal's project, a trashed one, or one the caller may not see are all "not found". */
	private Found findVisible(UUID portalId, UUID projectId, Caller caller) {
		return findVisible(portalId, projectId, caller, false);
	}

	/** With {@code lock}, the project row stays locked until the transaction ends (used by edits). */
	private Found findVisible(UUID portalId, UUID projectId, Caller caller, boolean lock) {
		Project project = (lock ? this.projectRepository.lockByIdAndPortalId(projectId, portalId)
				: this.projectRepository.findByIdAndPortalId(projectId, portalId))
			.filter((found) -> !found.isTrashed())
			.orElseThrow(this::notFound);
		ProjectRole role = roleOf(portalId, projectId, caller.userId());
		if (!canSee(project, caller, role)) {
			throw notFound();
		}
		return new Found(project, role);
	}

	/** The owner and new members must be active people of this portal (Work's copy of Platform's members). */
	private void requireActivePerson(UUID portalId, UUID userId) {
		boolean active = this.accessLookUpService.findMemberAccess(portalId, userId)
			.filter(MemberAccessResponseDTO::active)
			.isPresent();
		if (!active) {
			throw new AppException(HttpStatus.BAD_REQUEST, ProjectConstants.ERROR_PERSON_INVALID,
					ProjectConstants.MESSAGE_PERSON_INVALID);
		}
	}

	/** A field-level validation error: the property name under {@code details}, like bean validation gives. */
	private static AppException invalid(String property, String message) {
		Map<String, Object> details = new HashMap<>();
		details.put(property, message);
		return new AppException(HttpStatus.BAD_REQUEST, AppConstants.ERROR_VALIDATION_FAILED,
				AppConstants.MESSAGE_VALIDATION_FAILED, details);
	}

	private static void requireDates(LocalDate start, LocalDate end) {
		if (start != null && end != null && end.isBefore(start)) {
			throw new AppException(HttpStatus.BAD_REQUEST, ProjectConstants.ERROR_DATES_INVALID,
					ProjectConstants.MESSAGE_DATES_INVALID);
		}
	}

	/* ---------- helpers ---------- */

	private ProjectRole roleOf(UUID portalId, UUID projectId, UUID userId) {
		return this.memberRepository.findByPortalIdAndProjectIdAndUserId(portalId, projectId, userId)
			.map(ProjectMember::getRole)
			.orElse(null);
	}

	private Map<UUID, ProjectRole> rolesOf(UUID portalId, UUID userId) {
		return this.memberRepository.findByPortalIdAndUserId(portalId, userId)
			.stream()
			.collect(Collectors.toMap(ProjectMember::getProjectId, ProjectMember::getRole));
	}

	private Map<UUID, Integer> memberCounts(UUID portalId) {
		return this.memberRepository.countByProject(portalId)
			.stream()
			.collect(Collectors.toMap((count) -> count.getProjectId(), (count) -> (int) count.getTotal()));
	}

	private ProjectResponseDTO view(Project project, Caller caller, Map<UUID, ProjectRole> roles,
			Map<UUID, Integer> counts, Set<String> activeKeys) {
		ProjectRole role = roles.get(project.getId());
		return this.mapper.toResponse(project, counts.getOrDefault(project.getId(), 0), role != null,
				canEdit(project, caller, role), canDelete(project, caller), activeValues(project, activeKeys));
	}

	/** The api_names of the portal's active project fields: only their answers are ever returned. */
	/** The layout must be in this portal (else "not found") and active (a project keeps one that is deactivated later). */
	private LayoutResponseDTO requireActiveLayout(UUID portalId, UUID layoutId, String entityType) {
		LayoutResponseDTO layout = this.layoutLookUpService.getLayout(portalId, layoutId);
		if (!entityType.equals(layout.entityType())) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, ProjectConstants.ERROR_LAYOUT_INVALID,
					ProjectConstants.MESSAGE_LAYOUT_WRONG_MODULE);
		}
		if (!layout.active()) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, ProjectConstants.ERROR_LAYOUT_INACTIVE,
					ProjectConstants.MESSAGE_LAYOUT_INACTIVE);
		}
		return layout;
	}

	private Set<String> activeFieldKeys(UUID portalId) {
		return this.customFieldLookUpService.findFields(portalId, ProjectConstants.CUSTOM_FIELD_ENTITY)
			.stream()
			.map(CustomFieldResponseDTO::apiName)
			.collect(Collectors.toSet());
	}

	/** Answers of active fields only; keys of inactive or deleted fields are never shown. */
	private static Map<String, Object> activeValues(Project project, Collection<String> activeKeys) {
		Map<String, Object> values = new HashMap<>();
		project.getCustomValues().forEach((key, value) -> {
			if (activeKeys.contains(key)) {
				values.put(key, value);
			}
		});
		return values;
	}

	private ProjectResponseDTO viewOne(Project project, Caller caller, ProjectRole role, UUID portalId) {
		int count = this.memberRepository.findByPortalIdAndProjectId(portalId, project.getId()).size();
		return this.mapper.toResponse(project, count, role != null, canEdit(project, caller, role),
				canDelete(project, caller), activeValues(project, activeFieldKeys(portalId)));
	}

	/** Owner first, then Managers, then the rest. */
	private List<ProjectMemberResponseDTO> memberResponses(Project project, List<ProjectMember> members) {
		return members.stream()
			.sorted(Comparator.comparing((ProjectMember m) -> !m.getUserId().equals(project.getOwnerUserId()))
				.thenComparing((ProjectMember m) -> m.getRole() != ProjectRole.MANAGER)
				.thenComparing(ProjectMember::getAddedAt))
			.map((member) -> this.mapper.toMember(member, project))
			.toList();
	}

	private void ensureManager(UUID portalId, UUID projectId, UUID userId, Instant now) {
		ProjectMember existing = this.memberRepository.findByPortalIdAndProjectIdAndUserId(portalId, projectId, userId)
			.orElse(null);
		if (existing == null) {
			this.memberRepository.save(new ProjectMember(portalId, projectId, userId, ProjectRole.MANAGER, now));
		}
		else {
			existing.changeRole(ProjectRole.MANAGER);
		}
	}

	private static String clean(String text) {
		return text == null ? "" : text.trim();
	}

	private static void addIf(List<String> changes, String name, boolean changed) {
		if (changed) {
			changes.add(name);
		}
	}

	private void record(Project project, String action, UUID actorId, String actorName, UUID targetUserId,
			String detail) {
		this.activityRepository.save(new ProjectActivity(project.getPortalId(), project.getId(), action, actorId,
				actorName, targetUserId, detail, this.clock.instant()));
	}

	/** Same transaction as the change; the relay sends it to Kafka afterwards (never for a rolled-back change). */
	private void publish(String eventType, Project project, UUID actorId, String actorName, Map<String, Object> extra) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("portalId", project.getPortalId());
		payload.put("projectId", project.getId());
		payload.put("key", project.getProjectKey());
		payload.putAll(extra);
		this.outboxWriteService.publish(new OutboxEventCreateDTO(project.getPortalId(),
				OutboxEventConstants.AGGREGATE_PROJECT, project.getId(), eventType, actorId, actorName, payload));
		// Every project write publishes, so this is also the one place that drops what was cached about the project.
		this.projectCache.projectChanged(project.getPortalId(), project.getId());
	}

	private AppException notFound() {
		return new AppException(HttpStatus.NOT_FOUND, ProjectConstants.ERROR_PROJECT_NOT_FOUND,
				ProjectConstants.MESSAGE_PROJECT_NOT_FOUND);
	}

	private static AppException forbidden(String message) {
		return new AppException(HttpStatus.FORBIDDEN, ProjectConstants.ERROR_PROJECT_FORBIDDEN, message);
	}

	private static AppException keyTaken() {
		return new AppException(HttpStatus.CONFLICT, ProjectConstants.ERROR_KEY_TAKEN,
				ProjectConstants.MESSAGE_KEY_TAKEN);
	}

}

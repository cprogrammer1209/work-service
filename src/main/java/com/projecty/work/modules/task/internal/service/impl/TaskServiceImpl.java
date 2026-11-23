package com.projecty.work.modules.task.internal.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.exception.AppException;
import com.projecty.work.common.service.RequestPortalContext;
import com.projecty.work.modules.customfield.api.CustomFieldLookUpService;
import com.projecty.work.modules.customfield.api.CustomFieldWriteService;
import com.projecty.work.modules.customfield.api.LayoutLookUpService;
import com.projecty.work.modules.customfield.api.dto.LayoutFieldResponseDTO;
import com.projecty.work.modules.customfield.api.dto.LayoutResponseDTO;
import com.projecty.work.modules.customfield.api.dto.LayoutStatusResponseDTO;
import com.projecty.work.modules.project.api.dto.ProjectSummaryDTO;
import com.projecty.work.modules.shared.constants.OutboxEventConstants;
import com.projecty.work.modules.shared.constants.PermissionConstants;
import com.projecty.work.modules.task.internal.constants.TaskConstants;
import com.projecty.work.modules.task.internal.dto.TaskAcrossFilterDTO;
import com.projecty.work.modules.task.internal.dto.TaskActivityResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskCommentResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskCreateDTO;
import com.projecty.work.modules.task.internal.dto.TaskDetailResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskFilterDTO;
import com.projecty.work.modules.task.internal.dto.TaskPageResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskUpdateDTO;
import com.projecty.work.modules.task.internal.entity.Task;
import com.projecty.work.modules.task.internal.entity.TaskList;
import com.projecty.work.modules.task.internal.entity.TaskListVisibility;
import com.projecty.work.modules.task.internal.entity.TaskPriority;
import com.projecty.work.modules.task.internal.helper.TaskAccess;
import com.projecty.work.modules.task.internal.helper.TaskCaller;
import com.projecty.work.modules.task.internal.helper.TaskHistory;
import com.projecty.work.modules.task.internal.helper.TaskInputChecker;
import com.projecty.work.modules.task.internal.helper.TaskViewBuilder;
import com.projecty.work.modules.task.internal.mapper.TaskMapper;
import com.projecty.work.modules.task.internal.repository.TaskActivityRepository;
import com.projecty.work.modules.task.internal.repository.TaskCommentRepository;
import com.projecty.work.modules.task.internal.repository.TaskListRepository;
import com.projecty.work.modules.task.internal.repository.TaskRepository;
import com.projecty.work.modules.task.internal.service.TaskListService;
import com.projecty.work.modules.task.internal.service.TaskService;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskServiceImpl implements TaskService {

	private final TaskRepository taskRepository;

	private final TaskListRepository listRepository;

	private final TaskCommentRepository commentRepository;

	private final TaskActivityRepository activityRepository;

	private final TaskListService listService;

	private final TaskAccess access;

	private final TaskHistory history;

	private final TaskViewBuilder views;

	private final CustomFieldLookUpService customFieldLookUpService;

	private final CustomFieldWriteService customFieldWriteService;

	private final LayoutLookUpService layoutLookUpService;

	private final RequestPortalContext portalContext;

	private final TaskMapper mapper;

	private final Clock clock;

	public TaskServiceImpl(TaskRepository taskRepository, TaskListRepository listRepository,
			TaskCommentRepository commentRepository, TaskActivityRepository activityRepository,
			TaskListService listService, TaskAccess access, TaskHistory history, TaskViewBuilder views,
			CustomFieldLookUpService customFieldLookUpService, CustomFieldWriteService customFieldWriteService,
			LayoutLookUpService layoutLookUpService, RequestPortalContext portalContext, TaskMapper mapper,
			Clock clock) {
		this.taskRepository = taskRepository;
		this.listRepository = listRepository;
		this.commentRepository = commentRepository;
		this.activityRepository = activityRepository;
		this.listService = listService;
		this.access = access;
		this.history = history;
		this.views = views;
		this.customFieldLookUpService = customFieldLookUpService;
		this.customFieldWriteService = customFieldWriteService;
		this.layoutLookUpService = layoutLookUpService;
		this.portalContext = portalContext;
		this.mapper = mapper;
		this.clock = clock;
	}

	/** One changed field, for the history. */
	private record Change(String field, String oldValue, String newValue) {
	}

	/* ---------- reading ---------- */

	@Override
	@Transactional(readOnly = true)
	public List<TaskResponseDTO> list(UUID actorId, UUID projectId, TaskFilterDTO filter) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		ProjectSummaryDTO project = this.access.project(portalId, projectId, actorId);
		if (filter.trashed()) {
			this.access.require(caller, PermissionConstants.TASKS_DELETE, TaskConstants.MESSAGE_TRASH_FORBIDDEN);
		}
		String statusType = filter.statusType() == null || filter.statusType().isBlank() ? null
				: filter.statusType().trim().toUpperCase();
		if (statusType != null && !statusType.equals("OPEN") && !statusType.equals("CLOSED")) {
			throw invalid("statusType", "Use OPEN or CLOSED.");
		}
		List<Task> live = this.taskRepository.findByPortalIdAndProjectIdAndDeletedAtIsNull(portalId, project.id());
		List<Task> trashed = filter.trashed()
				? this.taskRepository.findByPortalIdAndProjectIdAndDeletedAtIsNotNull(portalId, project.id())
				: List.of();
		List<Task> all = new ArrayList<>(live);
		all.addAll(trashed);
		TaskViewBuilder.Context context = this.views.context(portalId, project, caller, all);
		List<Task> shown = filter.trashed() ? trashRoots(trashed) : live;
		String query = filter.q() == null || filter.q().isBlank() ? null : filter.q().trim().toLowerCase();
		String wantedType = statusType;
		Comparator<Task> order = filter.trashed()
				? Comparator.comparing(Task::getDeletedAt).reversed().thenComparing(Task::getTaskNumber)
				: Comparator.comparingInt((Task task) -> listOrder(context, task)).thenComparingInt(Task::getTaskNumber);
		return shown.stream()
			.filter((task) -> visibleToCaller(context, task))
			.filter((task) -> filter.listId() == null || filter.listId().equals(task.getTaskListId()))
			.filter((task) -> filter.statusId() == null || filter.statusId().equals(task.getStatusId()))
			.filter((task) -> wantedType == null || wantedType.equals(statusTypeOf(context, task)))
			.filter((task) -> filter.ownerId() == null || task.getOwnerIds().contains(filter.ownerId()))
			.filter((task) -> filter.priority() == null || filter.priority() == task.getPriority())
			.filter((task) -> filter.parentId() == null || filter.parentId().equals(task.getParentTaskId()))
			.filter((task) -> query == null || matches(task, project.key(), query))
			.sorted(order)
			.map((task) -> this.views.view(context, task))
			.toList();
	}

	/** A task with the project context it is shown in, so one sort can span several projects. */
	private record Row(Task task, TaskViewBuilder.Context context) {
	}

	@Override
	@Transactional(readOnly = true)
	public TaskPageResponseDTO listAcrossProjects(UUID actorId, TaskAcrossFilterDTO filter) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		String scope = filter.scope() == null || filter.scope().isBlank() ? TaskConstants.SCOPE_ALL
				: filter.scope().trim().toUpperCase();
		if (!scope.equals(TaskConstants.SCOPE_ALL) && !scope.equals(TaskConstants.SCOPE_MINE)) {
			throw invalid(TaskConstants.DETAIL_SCOPE, TaskConstants.MESSAGE_SCOPE_INVALID);
		}
		String statusType = filter.statusType() == null || filter.statusType().isBlank() ? null
				: filter.statusType().trim().toUpperCase();
		if (statusType != null && !statusType.equals(TaskConstants.STATUS_TYPE_OPEN)
				&& !statusType.equals(TaskConstants.STATUS_TYPE_CLOSED)) {
			throw invalid(TaskConstants.DETAIL_STATUS_TYPE, TaskConstants.MESSAGE_STATUS_TYPE_INVALID);
		}
		int limit = filter.limit() == null ? TaskConstants.ACROSS_LIMIT_DEFAULT : filter.limit();
		if (limit < 1 || limit > TaskConstants.ACROSS_LIMIT_MAX) {
			throw invalid(TaskConstants.DETAIL_LIMIT, TaskConstants.MESSAGE_LIMIT_INVALID);
		}
		int offset = 0;
		if (filter.cursor() != null && !filter.cursor().isBlank()) {
			try {
				offset = Integer.parseInt(filter.cursor().trim());
			}
			catch (NumberFormatException ex) {
				offset = -1;
			}
			if (offset < 0) {
				throw invalid(TaskConstants.DETAIL_CURSOR, TaskConstants.MESSAGE_CURSOR_INVALID);
			}
		}
		List<ProjectSummaryDTO> projects = new ArrayList<>();
		if (filter.projectId() != null) {
			projects.add(this.access.project(portalId, filter.projectId(), actorId));
		}
		else {
			for (UUID projectId : this.taskRepository.findProjectIdsWithLiveTasks(portalId)) {
				this.access.projectOrNull(portalId, projectId, actorId).ifPresent(projects::add);
			}
		}
		String query = filter.q() == null || filter.q().isBlank() ? null : filter.q().trim().toLowerCase();
		String wantedType = statusType;
		boolean mine = scope.equals(TaskConstants.SCOPE_MINE);
		List<Row> rows = new ArrayList<>();
		for (ProjectSummaryDTO project : projects) {
			List<Task> live = this.taskRepository.findByPortalIdAndProjectIdAndDeletedAtIsNull(portalId, project.id());
			TaskViewBuilder.Context context = this.views.context(portalId, project, caller, live);
			live.stream()
				.filter((task) -> visibleToCaller(context, task))
				.filter((task) -> !mine || task.getOwnerIds().contains(actorId))
				.filter((task) -> filter.ownerId() == null || task.getOwnerIds().contains(filter.ownerId()))
				.filter((task) -> filter.priority() == null || filter.priority() == task.getPriority())
				.filter((task) -> wantedType == null || wantedType.equals(statusTypeOf(context, task)))
				.filter((task) -> filter.dueBefore() == null
						|| task.getDueDate() != null && !task.getDueDate().isAfter(filter.dueBefore()))
				.filter((task) -> filter.dueAfter() == null
						|| task.getDueDate() != null && !task.getDueDate().isBefore(filter.dueAfter()))
				.filter((task) -> query == null || matches(task, project.key(), query))
				.forEach((task) -> rows.add(new Row(task, context)));
		}
		rows.sort(Comparator
			.comparing((Row row) -> row.task().getDueDate(), Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()))
			.thenComparing((Row row) -> row.context().project().key())
			.thenComparingInt((Row row) -> row.task().getTaskNumber()));
		int end = (int) Math.min((long) offset + limit, rows.size());
		List<TaskResponseDTO> items = offset >= rows.size() ? List.of()
				: rows.subList(offset, end).stream().map((row) -> this.views.view(row.context(), row.task())).toList();
		return new TaskPageResponseDTO(items, end < rows.size() ? String.valueOf(end) : null, rows.size());
	}

	@Override
	@Transactional(readOnly = true)
	public TaskDetailResponseDTO get(UUID actorId, UUID taskId) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		TaskAccess.Found found = this.access.find(portalId, taskId, caller, false, false);
		Task task = found.task();
		List<Task> live = this.taskRepository.findByPortalIdAndProjectIdAndDeletedAtIsNull(portalId, task.getProjectId());
		TaskViewBuilder.Context context = this.views.context(portalId, found.project(), caller, live);
		LayoutResponseDTO layout = this.layoutLookUpService.getLayout(portalId, task.getLayoutId());
		Set<String> placed = layout.sections()
			.stream()
			.flatMap((section) -> section.fields().stream())
			.map(LayoutFieldResponseDTO::apiName)
			.collect(Collectors.toSet());
		Set<String> hidden = new java.util.HashSet<>(context.activeKeys());
		hidden.removeAll(placed);
		List<TaskResponseDTO> subtasks = descendants(task, live).stream()
			.sorted(Comparator.comparingInt(Task::getDepth).thenComparingInt(Task::getTaskNumber))
			.map((subtask) -> this.views.view(context, subtask))
			.toList();
		List<TaskActivityResponseDTO> activity = this.activityRepository
			.findByPortalIdAndTaskIdOrderByCreatedAtDescIdDesc(portalId, taskId,
					PageRequest.of(0, TaskConstants.ACTIVITY_LIMIT))
			.stream()
			.map(this.mapper::toActivity)
			.toList();
		List<TaskCommentResponseDTO> comments = this.commentRepository
			.findByPortalIdAndTaskIdOrderByCreatedAtAscIdAsc(portalId, taskId)
			.stream()
			.map((comment) -> this.mapper.toComment(comment, comment.getAuthorUserId().equals(actorId),
					comment.getAuthorUserId().equals(actorId) || caller.admin()))
			.toList();
		return new TaskDetailResponseDTO(this.views.view(context, task, TaskViewBuilder.activeValues(task, placed)),
				layout, TaskViewBuilder.activeValues(task, hidden), layout.statuses(), subtasks, activity, comments,
				task.getFollowerIds());
	}

	/* ---------- creating ---------- */

	@Override
	@Transactional
	public TaskResponseDTO create(UUID actorId, String actorName, UUID projectId, TaskCreateDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		ProjectSummaryDTO project = this.access.project(portalId, projectId, actorId);
		TaskInputChecker checker = new TaskInputChecker();
		String title = checker.title(request.title());
		String description = checker.description(request.description());
		TaskPriority priority = request.priority() == null ? TaskPriority.NONE : request.priority();
		checker.dates(request.startDate(), request.dueDate());
		BigDecimal workHours = checker.workHours(request.workHours());
		int completion = request.completion() == null ? 0 : checker.completion(request.completion());
		List<String> tags = checker.tags(request.tags());
		List<UUID> owners = checker.owners(request.ownerIds());
		checker.verify();
		this.access.requireActivePeople(portalId, owners, TaskConstants.DETAIL_OWNERS);
		List<Task> live = this.taskRepository.findByPortalIdAndProjectIdAndDeletedAtIsNull(portalId, project.id());
		Task parent = null;
		if (request.parentId() != null) {
			parent = live.stream().filter((task) -> task.getId().equals(request.parentId())).findFirst()
				.orElseThrow(() -> invalid(TaskConstants.DETAIL_PARENT, TaskConstants.MESSAGE_PARENT_INVALID));
			if (parent.getDepth() + 1 > TaskConstants.MAX_DEPTH) {
				throw invalid(TaskConstants.DETAIL_PARENT, TaskConstants.MESSAGE_PARENT_TOO_DEEP);
			}
		}
		TaskList list = chooseList(portalId, project, actorId, request.listId(), parent);
		UUID layoutId = project.taskLayoutId() != null ? project.taskLayoutId()
				: this.layoutLookUpService.getDefaultLayoutId(portalId, TaskConstants.CUSTOM_FIELD_ENTITY);
		List<LayoutStatusResponseDTO> statuses = this.layoutLookUpService.findStatuses(portalId, layoutId);
		LayoutStatusResponseDTO status = request.statusId() == null
				? statuses.stream().filter(LayoutStatusResponseDTO::defaultStatus).findFirst().orElseThrow()
				: statuses.stream()
					.filter((candidate) -> candidate.id().equals(request.statusId()))
					.findFirst()
					.orElseThrow(() -> invalid(TaskConstants.DETAIL_STATUS, TaskConstants.MESSAGE_STATUS_INVALID));
		Map<String, Object> customValues = this.customFieldLookUpService.validate(portalId,
				TaskConstants.CUSTOM_FIELD_ENTITY, layoutId,
				request.customValues() == null ? Map.of() : request.customValues(), Map.of(), true);
		Instant now = this.clock.instant();
		int number = this.taskRepository.nextNumber(project.id(), portalId);
		Task task = new Task(portalId, project.id(), list.getId(), parent == null ? null : parent.getId(),
				parent == null ? 1 : parent.getDepth() + 1, number, title, layoutId, status.id(), actorId, now);
		task.changeDescription(description);
		task.changePriority(priority);
		task.changeDates(request.startDate(), request.dueDate());
		task.changeWorkHours(workHours);
		task.changeCompletion(completion);
		task.changeOwners(owners);
		task.changeTags(tags);
		task.changeCustomValues(customValues);
		List<UUID> followers = new ArrayList<>(List.of(actorId));
		owners.stream().filter((owner) -> !followers.contains(owner)).forEach(followers::add);
		task.changeFollowers(followers);
		if (TaskConstants.STATUS_TYPE_CLOSED.equals(status.statusType())) {
			task.changeStatus(status.id(), now);
			task.changeCompletion(TaskConstants.COMPLETION_MAX);
		}
		this.taskRepository.saveAndFlush(task);
		this.customFieldWriteService.reserveUniqueValues(portalId, TaskConstants.CUSTOM_FIELD_ENTITY, task.getId(),
				Map.of(), customValues);
		this.history.record(task, TaskConstants.ACTION_CREATED, null, null, null, "Created task", actorId, actorName);
		Map<String, Object> extra = new LinkedHashMap<>();
		extra.put("listId", list.getId());
		extra.put("parentId", task.getParentTaskId());
		extra.put("statusId", status.id());
		this.history.publish(OutboxEventConstants.EVENT_TASK_CREATED, task, project.key(), actorId, actorName, extra);
		return respond(portalId, project, caller, task);
	}

	/* ---------- changing ---------- */

	@Override
	@Transactional
	public TaskResponseDTO update(UUID actorId, String actorName, UUID taskId, TaskUpdateDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		TaskAccess.Found found = this.access.find(portalId, taskId, caller, true, false);
		Task task = found.task();
		ProjectSummaryDTO project = found.project();
		// Only what the request carries changes; everything else keeps its stored value.
		TaskInputChecker checker = new TaskInputChecker();
		String title = request.isTitleSet() ? checker.title(request.getTitle()) : task.getTitle();
		String description = request.isDescriptionSet() ? checker.description(request.getDescription())
				: task.getDescription();
		TaskPriority priority = request.isPrioritySet() ? checker.priority(request.getPriority()) : task.getPriority();
		LocalDate startDate = request.isStartDateSet() ? request.getStartDate() : task.getStartDate();
		LocalDate dueDate = request.isDueDateSet() ? request.getDueDate() : task.getDueDate();
		if (request.isStartDateSet() || request.isDueDateSet()) {
			checker.dates(startDate, dueDate);
		}
		BigDecimal workHours = request.isWorkHoursSet() ? checker.workHours(request.getWorkHours()) : task.getWorkHours();
		int completion = request.isCompletionSet() ? checker.completion(request.getCompletion()) : task.getCompletion();
		List<String> tags = request.isTagsSet() ? checker.tags(request.getTags()) : task.getTags();
		List<UUID> owners = request.isOwnerIdsSet() ? checker.owners(request.getOwnerIds()) : task.getOwnerIds();
		if (request.isStatusIdSet() && request.getStatusId() == null) {
			checker.problem(TaskConstants.DETAIL_STATUS, TaskConstants.MESSAGE_STATUS_REQUIRED);
		}
		if (request.isListIdSet() && request.getListId() == null) {
			checker.problem(TaskConstants.DETAIL_LIST, TaskConstants.MESSAGE_LIST_REQUIRED);
		}
		checker.verify();
		List<UUID> addedOwners = owners.stream().filter((owner) -> !task.getOwnerIds().contains(owner)).toList();
		this.access.requireActivePeople(portalId, addedOwners, TaskConstants.DETAIL_OWNERS);

		List<Task> live = this.taskRepository.findByPortalIdAndProjectIdAndDeletedAtIsNull(portalId, project.id());
		Map<UUID, Task> byId = live.stream().collect(Collectors.toMap(Task::getId, Function.identity()));
		List<Task> subtree = descendants(task, live);

		// Where the task sits: parent, depth and list (a subtask stays in its parent's list).
		UUID newParentId = request.isParentIdSet() ? request.getParentId() : task.getParentTaskId();
		boolean parentChanged = !Objects.equals(newParentId, task.getParentTaskId());
		Task newParent = newParentId == null ? null : byId.get(newParentId);
		int newDepth = task.getDepth();
		if (parentChanged) {
			if (newParentId != null) {
				if (newParent == null) {
					throw invalid(TaskConstants.DETAIL_PARENT, TaskConstants.MESSAGE_PARENT_INVALID);
				}
				if (newParent.getId().equals(task.getId()) || subtree.contains(newParent)) {
					throw invalid(TaskConstants.DETAIL_PARENT, TaskConstants.MESSAGE_PARENT_CYCLE);
				}
			}
			newDepth = newParent == null ? 1 : newParent.getDepth() + 1;
			int height = subtree.stream().mapToInt(Task::getDepth).max().orElse(task.getDepth()) - task.getDepth();
			if (newDepth + height > TaskConstants.MAX_DEPTH) {
				throw invalid(TaskConstants.DETAIL_PARENT, TaskConstants.MESSAGE_PARENT_TOO_DEEP);
			}
		}
		Task effectiveParent = parentChanged ? newParent
				: (task.getParentTaskId() == null ? null : byId.get(task.getParentTaskId()));
		UUID newListId = task.getTaskListId();
		if (request.isListIdSet()) {
			TaskList target = requireList(portalId, project.id(), request.getListId());
			if (effectiveParent != null && !effectiveParent.getTaskListId().equals(target.getId())) {
				throw invalid(TaskConstants.DETAIL_LIST, TaskConstants.MESSAGE_SUBTASK_LIST);
			}
			if (target.isArchived() && !target.getId().equals(task.getTaskListId())) {
				throw archived();
			}
			newListId = target.getId();
		}
		else if (parentChanged && newParent != null) {
			newListId = newParent.getTaskListId();
		}

		// The status must be one of the task's own layout.
		LayoutStatusResponseDTO oldStatus = null;
		LayoutStatusResponseDTO newStatus = null;
		if (request.isStatusIdSet()) {
			List<LayoutStatusResponseDTO> statuses = this.layoutLookUpService.findStatuses(portalId, task.getLayoutId());
			oldStatus = statuses.stream().filter((s) -> s.id().equals(task.getStatusId())).findFirst().orElse(null);
			newStatus = statuses.stream()
				.filter((s) -> s.id().equals(request.getStatusId()))
				.findFirst()
				.orElseThrow(() -> invalid(TaskConstants.DETAIL_STATUS, TaskConstants.MESSAGE_STATUS_INVALID));
		}

		// The row is locked, so what we read here is what we replace: concurrent edits queue instead of overwriting.
		Map<String, Object> before = new HashMap<>(task.getCustomValues());
		Map<String, Object> after = before;
		if (request.isCustomValuesSet() && request.getCustomValues() != null) {
			after = this.customFieldLookUpService.validate(portalId, TaskConstants.CUSTOM_FIELD_ENTITY,
					task.getLayoutId(), request.getCustomValues(), before, false);
		}

		List<Change> changes = new ArrayList<>();
		addIf(changes, TaskConstants.FIELD_TITLE, task.getTitle(), title);
		addIf(changes, TaskConstants.FIELD_DESCRIPTION, task.getDescription(), description);
		addIf(changes, TaskConstants.FIELD_PRIORITY, task.getPriority(), priority);
		addIf(changes, "startDate", task.getStartDate(), startDate);
		addIf(changes, "dueDate", task.getDueDate(), dueDate);
		boolean hoursChanged = !sameHours(task.getWorkHours(), workHours);
		if (hoursChanged) {
			changes.add(new Change(TaskConstants.FIELD_WORK_HOURS, text(task.getWorkHours()), text(workHours)));
		}
		addIf(changes, TaskConstants.FIELD_OWNERS, join(task.getOwnerIds()), join(owners));
		addIf(changes, TaskConstants.FIELD_TAGS, String.join(", ", task.getTags()), String.join(", ", tags));
		if (parentChanged) {
			changes.add(new Change(TaskConstants.FIELD_PARENT, text(task.getParentTaskId()), text(newParentId)));
		}
		if (!newListId.equals(task.getTaskListId())) {
			changes.add(new Change(TaskConstants.FIELD_LIST, text(task.getTaskListId()), text(newListId)));
		}
		Set<String> changedKeys = this.customFieldLookUpService.changedKeys(before, after);
		for (String key : changedKeys) {
			changes.add(new Change(TaskConstants.FIELD_CUSTOM_PREFIX + key, text(before.get(key)), text(after.get(key))));
		}
		boolean statusChanged = newStatus != null && !newStatus.id().equals(task.getStatusId());
		// Closing sets completion to 100 and wins over a completion sent in the same request.
		boolean closing = statusChanged && TaskConstants.STATUS_TYPE_CLOSED.equals(newStatus.statusType());
		if (!closing) {
			addIf(changes, TaskConstants.FIELD_COMPLETION, task.getCompletion(), completion);
		}

		Instant now = this.clock.instant();
		task.changeTitle(title);
		task.changeDescription(description);
		task.changePriority(priority);
		task.changeDates(startDate, dueDate);
		task.changeWorkHours(workHours);
		if (!closing) {
			task.changeCompletion(completion);
		}
		task.changeTags(tags);
		if (!owners.equals(task.getOwnerIds())) {
			List<UUID> followers = new ArrayList<>(task.getFollowerIds());
			addedOwners.stream().filter((owner) -> !followers.contains(owner)).forEach(followers::add);
			task.changeOwners(owners);
			task.changeFollowers(followers);
		}
		if (parentChanged) {
			int delta = newDepth - task.getDepth();
			task.changeParent(newParentId, newDepth);
			subtree.forEach((subtask) -> subtask.changeDepth(subtask.getDepth() + delta));
		}
		if (!newListId.equals(task.getTaskListId())) {
			task.changeList(newListId);
			UUID listId = newListId;
			subtree.forEach((subtask) -> subtask.changeList(listId));
		}
		if (after != before) {
			task.changeCustomValues(after);
			this.customFieldWriteService.reserveUniqueValues(portalId, TaskConstants.CUSTOM_FIELD_ENTITY, task.getId(),
					before, after);
		}
		if (!changes.isEmpty() || statusChanged) {
			task.touch(actorId, now);
		}
		for (Change change : changes) {
			this.history.record(task, TaskConstants.ACTION_UPDATED, change.field(), change.oldValue(),
					change.newValue(), null, actorId, actorName);
		}
		if (!changes.isEmpty()) {
			this.history.publish(OutboxEventConstants.EVENT_TASK_UPDATED, task, project.key(), actorId, actorName,
					Map.of("changes", changes.stream().map(Change::field).toList()));
		}
		if (statusChanged) {
			moveToStatus(task, oldStatus, newStatus, now, actorId, actorName, project.key());
		}
		return respond(portalId, project, caller, task);
	}

	@Override
	@Transactional
	public TaskResponseDTO changeStatus(UUID actorId, String actorName, UUID taskId, UUID statusId) {
		TaskUpdateDTO request = new TaskUpdateDTO();
		request.setStatusId(statusId);
		return update(actorId, actorName, taskId, request);
	}

	@Override
	@Transactional
	public TaskDetailResponseDTO changeLayout(UUID actorId, String actorName, UUID taskId, UUID layoutId) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		TaskAccess.Found found = this.access.find(portalId, taskId, caller, true, false);
		Task task = found.task();
		LayoutResponseDTO target = this.layoutLookUpService.getLayout(portalId, layoutId);
		if (!TaskConstants.CUSTOM_FIELD_ENTITY.equals(target.entityType())) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, TaskConstants.ERROR_LAYOUT_INVALID,
					TaskConstants.MESSAGE_LAYOUT_WRONG_MODULE);
		}
		if (!target.active()) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, TaskConstants.ERROR_LAYOUT_INVALID,
					TaskConstants.MESSAGE_LAYOUT_INACTIVE);
		}
		if (!task.getLayoutId().equals(layoutId)) {
			LayoutResponseDTO current = this.layoutLookUpService.getLayout(portalId, task.getLayoutId());
			LayoutStatusResponseDTO oldStatus = current.statuses()
				.stream()
				.filter((status) -> status.id().equals(task.getStatusId()))
				.findFirst()
				.orElse(null);
			LayoutStatusResponseDTO newStatus = target.statuses()
				.stream()
				.filter((status) -> oldStatus != null && status.name().equalsIgnoreCase(oldStatus.name()))
				.findFirst()
				.orElseGet(() -> target.statuses().stream().filter(LayoutStatusResponseDTO::defaultStatus).findFirst().orElseThrow());
			Instant now = this.clock.instant();
			task.changeLayout(layoutId);
			task.touch(actorId, now);
			this.history.record(task, TaskConstants.ACTION_UPDATED, TaskConstants.FIELD_LAYOUT, current.name(),
					target.name(), null, actorId, actorName);
			this.history.publish(OutboxEventConstants.EVENT_TASK_UPDATED, task, found.project().key(), actorId,
					actorName, Map.of("changes", List.of(TaskConstants.FIELD_LAYOUT), "fromLayoutId", current.id(),
							"toLayoutId", target.id()));
			if (!newStatus.id().equals(task.getStatusId())) {
				moveToStatus(task, oldStatus, newStatus, now, actorId, actorName, found.project().key());
			}
			this.taskRepository.flush();
		}
		return get(actorId, taskId);
	}

	@Override
	@Transactional
	public void trash(UUID actorId, String actorName, UUID taskId) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		TaskAccess.Found found = this.access.find(portalId, taskId, caller, true, false);
		List<Task> live = this.taskRepository.findByPortalIdAndProjectIdAndDeletedAtIsNull(portalId,
				found.task().getProjectId());
		List<Task> trashing = new ArrayList<>();
		trashing.add(found.task());
		trashing.addAll(descendants(found.task(), live));
		Instant now = this.clock.instant();
		for (Task task : trashing) {
			task.trash(actorId, now);
			this.history.record(task, TaskConstants.ACTION_TRASHED, null, null, null, "Moved to trash", actorId,
					actorName);
			this.history.publish(OutboxEventConstants.EVENT_TASK_TRASHED, task, found.project().key(), actorId,
					actorName, Map.of("withParent", task != found.task()));
		}
	}

	@Override
	@Transactional
	public TaskResponseDTO restore(UUID actorId, String actorName, UUID taskId) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		TaskAccess.Found found = this.access.find(portalId, taskId, caller, true, true);
		Task task = found.task();
		if (!task.isTrashed()) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, TaskConstants.ERROR_NOT_TRASHED,
					TaskConstants.MESSAGE_NOT_TRASHED);
		}
		if (task.getParentTaskId() != null
				&& this.taskRepository.findByIdAndPortalId(task.getParentTaskId(), portalId)
					.map(Task::isTrashed)
					.orElse(false)) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, TaskConstants.ERROR_PARENT_TRASHED,
					TaskConstants.MESSAGE_PARENT_TRASHED);
		}
		// The task comes back with the subtasks that were trashed together with it (same moment).
		List<Task> trashed = this.taskRepository.findByPortalIdAndProjectIdAndDeletedAtIsNotNull(portalId,
				task.getProjectId());
		List<Task> restoring = new ArrayList<>();
		restoring.add(task);
		for (int i = 0; i < restoring.size(); i++) {
			UUID parentId = restoring.get(i).getId();
			for (Task candidate : trashed) {
				if (parentId.equals(candidate.getParentTaskId())
						&& Objects.equals(candidate.getDeletedAt(), task.getDeletedAt())) {
					restoring.add(candidate);
				}
			}
		}
		Instant now = this.clock.instant();
		for (Task restored : restoring) {
			restored.restore(actorId, now);
			this.history.record(restored, TaskConstants.ACTION_RESTORED, null, null, null, "Restored from trash",
					actorId, actorName);
			this.history.publish(OutboxEventConstants.EVENT_TASK_RESTORED, restored, found.project().key(), actorId,
					actorName, Map.of("withParent", restored != task));
		}
		return respond(portalId, found.project(), caller, task);
	}

	@Override
	@Transactional
	public TaskResponseDTO addFollower(UUID actorId, String actorName, UUID taskId, UUID userId) {
		return changeFollowers(actorId, actorName, taskId, userId, true);
	}

	@Override
	@Transactional
	public TaskResponseDTO removeFollower(UUID actorId, String actorName, UUID taskId, UUID userId) {
		return changeFollowers(actorId, actorName, taskId, userId, false);
	}

	/* ---------- helpers ---------- */

	private TaskResponseDTO changeFollowers(UUID actorId, String actorName, UUID taskId, UUID userId, boolean add) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		TaskAccess.Found found = this.access.find(portalId, taskId, caller, true, false);
		Task task = found.task();
		if (!userId.equals(actorId)) {
			this.access.require(caller, PermissionConstants.TASKS_EDIT, TaskConstants.MESSAGE_FOLLOWER_FORBIDDEN);
		}
		if (add) {
			this.access.requireActivePeople(portalId, List.of(userId), TaskConstants.DETAIL_USER);
		}
		List<UUID> followers = new ArrayList<>(task.getFollowerIds());
		boolean changed = add ? (!followers.contains(userId) && followers.add(userId)) : followers.remove(userId);
		if (changed) {
			String before = join(task.getFollowerIds());
			task.changeFollowers(followers);
			task.touch(actorId, this.clock.instant());
			this.history.record(task, TaskConstants.ACTION_UPDATED, TaskConstants.FIELD_FOLLOWERS, before,
					join(followers), null, actorId, actorName);
			this.history.publish(OutboxEventConstants.EVENT_TASK_UPDATED, task, found.project().key(), actorId,
					actorName, Map.of("changes", List.of(TaskConstants.FIELD_FOLLOWERS)));
		}
		return respond(portalId, found.project(), caller, task);
	}

	/**
	 * Puts the task in the new status: closing sets {@code closedAt} and completion 100, reopening clears
	 * {@code closedAt}. Writes the history and the events (status changed, and closed or reopened).
	 */
	private void moveToStatus(Task task, LayoutStatusResponseDTO oldStatus, LayoutStatusResponseDTO newStatus,
			Instant now, UUID actorId, String actorName, String projectKey) {
		boolean wasClosed = oldStatus != null && TaskConstants.STATUS_TYPE_CLOSED.equals(oldStatus.statusType());
		boolean closed = TaskConstants.STATUS_TYPE_CLOSED.equals(newStatus.statusType());
		Instant closedAt = closed ? (task.getClosedAt() != null ? task.getClosedAt() : now) : null;
		int oldCompletion = task.getCompletion();
		task.changeStatus(newStatus.id(), closedAt);
		if (closed && oldCompletion != TaskConstants.COMPLETION_MAX) {
			task.changeCompletion(TaskConstants.COMPLETION_MAX);
			this.history.record(task, TaskConstants.ACTION_UPDATED, TaskConstants.FIELD_COMPLETION,
					String.valueOf(oldCompletion), String.valueOf(TaskConstants.COMPLETION_MAX), null, actorId, actorName);
		}
		task.touch(actorId, now);
		this.history.record(task, TaskConstants.ACTION_STATUS_CHANGED, TaskConstants.FIELD_STATUS,
				oldStatus == null ? null : oldStatus.name(), newStatus.name(), null, actorId, actorName);
		Map<String, Object> extra = new LinkedHashMap<>();
		extra.put("fromStatusId", oldStatus == null ? null : oldStatus.id());
		extra.put("toStatusId", newStatus.id());
		this.history.publish(OutboxEventConstants.EVENT_TASK_STATUS_CHANGED, task, projectKey, actorId, actorName, extra);
		if (closed && !wasClosed) {
			this.history.publish(OutboxEventConstants.EVENT_TASK_CLOSED, task, projectKey, actorId, actorName, Map.of());
		}
		else if (!closed && wasClosed) {
			this.history.publish(OutboxEventConstants.EVENT_TASK_REOPENED, task, projectKey, actorId, actorName, Map.of());
		}
	}

	/** A subtask goes into its parent's list; else the chosen list (must be active); else the project's default list. */
	private TaskList chooseList(UUID portalId, ProjectSummaryDTO project, UUID actorId, UUID listId, Task parent) {
		if (parent != null) {
			return this.listRepository.findByIdAndPortalId(parent.getTaskListId(), portalId)
				.orElseThrow(TaskServiceImpl::listNotFound);
		}
		if (listId == null) {
			return this.listService.defaultList(portalId, project.id(), actorId);
		}
		TaskList list = requireList(portalId, project.id(), listId);
		if (list.isArchived()) {
			throw archived();
		}
		return list;
	}

	private TaskList requireList(UUID portalId, UUID projectId, UUID listId) {
		return this.listRepository.findByIdAndPortalId(listId, portalId)
			.filter((list) -> list.getProjectId().equals(projectId))
			.orElseThrow(TaskServiceImpl::listNotFound);
	}

	private TaskResponseDTO respond(UUID portalId, ProjectSummaryDTO project, TaskCaller caller, Task task) {
		List<Task> tasks = new ArrayList<>(
				this.taskRepository.findByPortalIdAndProjectIdAndDeletedAtIsNull(portalId, project.id()));
		if (tasks.stream().noneMatch((candidate) -> candidate.getId().equals(task.getId()))) {
			tasks.add(task);
		}
		return this.views.view(this.views.context(portalId, project, caller, tasks), task);
	}

	/** Every subtask at any depth. */
	private static List<Task> descendants(Task root, List<Task> tasks) {
		Map<UUID, List<Task>> children = new HashMap<>();
		for (Task task : tasks) {
			if (task.getParentTaskId() != null) {
				children.computeIfAbsent(task.getParentTaskId(), (id) -> new ArrayList<>()).add(task);
			}
		}
		List<Task> found = new ArrayList<>();
		List<Task> queue = new ArrayList<>(children.getOrDefault(root.getId(), List.of()));
		for (int i = 0; i < queue.size(); i++) {
			Task next = queue.get(i);
			found.add(next);
			queue.addAll(children.getOrDefault(next.getId(), List.of()));
		}
		return found;
	}

	/** Trashed tasks that were trashed on their own or as the top of a trashed branch. */
	private static List<Task> trashRoots(List<Task> trashed) {
		Map<UUID, Task> byId = trashed.stream().collect(Collectors.toMap(Task::getId, Function.identity()));
		return trashed.stream().filter((task) -> {
			Task parent = task.getParentTaskId() == null ? null : byId.get(task.getParentTaskId());
			return parent == null || !Objects.equals(parent.getDeletedAt(), task.getDeletedAt());
		}).toList();
	}

	/** A Client never sees tasks of an INTERNAL list. */
	private static boolean visibleToCaller(TaskViewBuilder.Context context, Task task) {
		if (!context.caller().client()) {
			return true;
		}
		TaskList list = context.lists().get(task.getTaskListId());
		return list != null && list.getVisibility() == TaskListVisibility.EXTERNAL;
	}

	private static int listOrder(TaskViewBuilder.Context context, Task task) {
		TaskList list = context.lists().get(task.getTaskListId());
		return list == null ? Integer.MAX_VALUE : list.getSortOrder();
	}

	private static String statusTypeOf(TaskViewBuilder.Context context, Task task) {
		LayoutStatusResponseDTO status = context.statuses().get(task.getStatusId());
		return status == null ? null : status.statusType();
	}

	private static boolean matches(Task task, String projectKey, String query) {
		return task.getTitle().toLowerCase().contains(query)
				|| TaskMapper.displayKey(projectKey, task.getTaskNumber()).toLowerCase().contains(query)
				|| task.getTags().stream().anyMatch((tag) -> tag.toLowerCase().contains(query));
	}

	private static void addIf(List<Change> changes, String field, Object before, Object after) {
		if (!Objects.equals(before, after)) {
			changes.add(new Change(field, text(before), text(after)));
		}
	}

	private static boolean sameHours(BigDecimal a, BigDecimal b) {
		return a == null ? b == null : (b != null && a.compareTo(b) == 0);
	}

	private static String text(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private static String join(List<UUID> ids) {
		return ids.stream().map(UUID::toString).collect(Collectors.joining(", "));
	}

	private static AppException invalid(String property, String message) {
		Map<String, Object> details = new HashMap<>();
		details.put(property, message);
		return new AppException(HttpStatus.BAD_REQUEST, AppConstants.ERROR_VALIDATION_FAILED,
				AppConstants.MESSAGE_VALIDATION_FAILED, details);
	}

	private static AppException listNotFound() {
		return new AppException(HttpStatus.NOT_FOUND, TaskConstants.ERROR_TASK_LIST_NOT_FOUND,
				TaskConstants.MESSAGE_TASK_LIST_NOT_FOUND);
	}

	private static AppException archived() {
		return new AppException(HttpStatus.UNPROCESSABLE_CONTENT, TaskConstants.ERROR_LIST_ARCHIVED,
				TaskConstants.MESSAGE_LIST_ARCHIVED);
	}

}

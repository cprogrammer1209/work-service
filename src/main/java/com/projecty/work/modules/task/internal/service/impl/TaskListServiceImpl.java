package com.projecty.work.modules.task.internal.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.exception.AppException;
import com.projecty.work.common.service.RequestPortalContext;
import com.projecty.work.modules.customfield.api.dto.LayoutStatusResponseDTO;
import com.projecty.work.modules.project.api.dto.ProjectSummaryDTO;
import com.projecty.work.modules.shared.constants.OutboxEventConstants;
import com.projecty.work.modules.task.internal.constants.TaskConstants;
import com.projecty.work.modules.task.internal.dto.TaskListCreateDTO;
import com.projecty.work.modules.task.internal.dto.TaskListResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskListUpdateDTO;
import com.projecty.work.modules.task.internal.dto.TaskOrderDTO;
import com.projecty.work.modules.task.internal.entity.Task;
import com.projecty.work.modules.task.internal.entity.TaskList;
import com.projecty.work.modules.task.internal.entity.TaskListVisibility;
import com.projecty.work.modules.task.internal.helper.TaskAccess;
import com.projecty.work.modules.task.internal.helper.TaskCaller;
import com.projecty.work.modules.task.internal.helper.TaskHistory;
import com.projecty.work.modules.task.internal.helper.TaskStatusResolver;
import com.projecty.work.modules.task.internal.mapper.TaskMapper;
import com.projecty.work.modules.task.internal.repository.TaskListRepository;
import com.projecty.work.modules.task.internal.repository.TaskRepository;
import com.projecty.work.modules.task.internal.service.TaskListService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskListServiceImpl implements TaskListService {

	private final TaskListRepository listRepository;

	private final TaskRepository taskRepository;

	private final TaskAccess access;

	private final TaskHistory history;

	private final TaskStatusResolver statusResolver;

	private final RequestPortalContext portalContext;

	private final TaskMapper mapper;

	private final Clock clock;

	public TaskListServiceImpl(TaskListRepository listRepository, TaskRepository taskRepository, TaskAccess access,
			TaskHistory history, TaskStatusResolver statusResolver, RequestPortalContext portalContext,
			TaskMapper mapper, Clock clock) {
		this.listRepository = listRepository;
		this.taskRepository = taskRepository;
		this.access = access;
		this.history = history;
		this.statusResolver = statusResolver;
		this.portalContext = portalContext;
		this.mapper = mapper;
		this.clock = clock;
	}

	/** Not read-only: the first call for a project creates its default list. */
	@Override
	@Transactional
	public List<TaskListResponseDTO> list(UUID actorId, UUID projectId, boolean includeArchived) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		ProjectSummaryDTO project = this.access.project(portalId, projectId, actorId);
		defaultList(portalId, project.id(), actorId);
		return respond(portalId, project.id(), caller, includeArchived);
	}

	@Override
	@Transactional
	public TaskListResponseDTO create(UUID actorId, String actorName, UUID projectId, TaskListCreateDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		ProjectSummaryDTO project = this.access.project(portalId, projectId, actorId);
		List<TaskList> lists = this.listRepository
			.findByPortalIdAndProjectIdOrderBySortOrderAscCreatedAtAscIdAsc(portalId, project.id());
		String name = requireName(request.name(), lists, null);
		TaskListVisibility visibility = request.visibility() == null ? TaskListVisibility.INTERNAL : request.visibility();
		int order = lists.stream().mapToInt(TaskList::getSortOrder).max().orElse(-1) + 1;
		TaskList list = this.listRepository
			.saveAndFlush(new TaskList(portalId, project.id(), name, visibility, order, actorId, this.clock.instant()));
		this.history.publishList(OutboxEventConstants.EVENT_TASK_LIST_CREATED, list, actorId, actorName, "created");
		return single(portalId, list, caller);
	}

	@Override
	@Transactional
	public TaskListResponseDTO update(UUID actorId, String actorName, UUID listId, TaskListUpdateDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		TaskList list = find(portalId, listId, caller);
		String name = list.getName();
		if (request.name() != null) {
			List<TaskList> lists = this.listRepository
				.findByPortalIdAndProjectIdOrderBySortOrderAscCreatedAtAscIdAsc(portalId, list.getProjectId());
			name = requireName(request.name(), lists, list.getId());
		}
		TaskListVisibility visibility = request.visibility() == null ? list.getVisibility() : request.visibility();
		if (!name.equals(list.getName()) || visibility != list.getVisibility()) {
			list.edit(name, visibility, this.clock.instant());
			this.listRepository.flush();
			this.history.publishList(OutboxEventConstants.EVENT_TASK_LIST_UPDATED, list, actorId, actorName, "updated");
		}
		return single(portalId, list, caller);
	}

	@Override
	@Transactional
	public TaskListResponseDTO archive(UUID actorId, String actorName, UUID listId, boolean archived) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		TaskList list = find(portalId, listId, caller);
		if (list.isArchived() != archived) {
			list.changeArchived(archived, this.clock.instant());
			this.history.publishList(
					archived ? OutboxEventConstants.EVENT_TASK_LIST_ARCHIVED : OutboxEventConstants.EVENT_TASK_LIST_UPDATED,
					list, actorId, actorName, archived ? "archived" : "unarchived");
		}
		return single(portalId, list, caller);
	}

	@Override
	@Transactional
	public List<TaskListResponseDTO> reorder(UUID actorId, String actorName, UUID projectId, TaskOrderDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		ProjectSummaryDTO project = this.access.project(portalId, projectId, actorId);
		Map<UUID, TaskList> lists = this.listRepository
			.findByPortalIdAndProjectIdOrderBySortOrderAscCreatedAtAscIdAsc(portalId, project.id())
			.stream()
			.collect(Collectors.toMap(TaskList::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
		Set<UUID> distinct = Set.copyOf(request.ids());
		if (distinct.size() != request.ids().size() || !lists.keySet().containsAll(distinct)) {
			Map<String, Object> details = new HashMap<>();
			details.put(TaskConstants.DETAIL_IDS, TaskConstants.MESSAGE_IDS_INVALID);
			throw new AppException(HttpStatus.BAD_REQUEST, AppConstants.ERROR_VALIDATION_FAILED,
					AppConstants.MESSAGE_VALIDATION_FAILED, details);
		}
		int order = 0;
		for (UUID id : request.ids()) {
			lists.get(id).changeSortOrder(order++);
		}
		for (TaskList list : lists.values()) {
			if (!distinct.contains(list.getId())) {
				list.changeSortOrder(order++);
			}
		}
		this.listRepository.flush();
		if (!request.ids().isEmpty()) {
			lists.values().stream().findFirst().ifPresent((first) -> this.history.publishList(
					OutboxEventConstants.EVENT_TASK_LIST_UPDATED, first, actorId, actorName, "reordered"));
		}
		return respond(portalId, project.id(), caller, true);
	}

	@Override
	@Transactional
	public TaskList defaultList(UUID portalId, UUID projectId, UUID userId) {
		List<TaskList> lists = this.listRepository
			.findByPortalIdAndProjectIdOrderBySortOrderAscCreatedAtAscIdAsc(portalId, projectId);
		TaskList active = lists.stream().filter((list) -> !list.isArchived()).findFirst().orElse(null);
		if (active != null) {
			return active;
		}
		Instant now = this.clock.instant();
		TaskList general = lists.stream()
			.filter((list) -> list.getName().equalsIgnoreCase(TaskConstants.DEFAULT_LIST_NAME))
			.findFirst()
			.orElse(null);
		if (general != null) {
			general.changeArchived(false, now);
			this.history.listsChanged(portalId, projectId);
			return general;
		}
		this.listRepository.insertDefaultIfMissing(portalId, projectId, TaskConstants.DEFAULT_LIST_NAME, userId, now);
		this.history.listsChanged(portalId, projectId);
		return this.listRepository.findByPortalIdAndProjectIdOrderBySortOrderAscCreatedAtAscIdAsc(portalId, projectId)
			.stream()
			.filter((list) -> !list.isArchived())
			.findFirst()
			.orElseThrow();
	}

	/* ---------- helpers ---------- */

	/** A list of a visible project; a Client does not find an INTERNAL list. Else "not found". */
	private TaskList find(UUID portalId, UUID listId, TaskCaller caller) {
		TaskList list = this.listRepository.findByIdAndPortalId(listId, portalId)
			.orElseThrow(TaskListServiceImpl::notFound);
		this.access.projectOrNull(portalId, list.getProjectId(), caller.userId()).orElseThrow(TaskListServiceImpl::notFound);
		if (caller.client() && list.getVisibility() != TaskListVisibility.EXTERNAL) {
			throw notFound();
		}
		return list;
	}

	private List<TaskListResponseDTO> respond(UUID portalId, UUID projectId, TaskCaller caller, boolean includeArchived) {
		Map<UUID, int[]> counts = counts(portalId, projectId);
		return this.listRepository.findByPortalIdAndProjectIdOrderBySortOrderAscCreatedAtAscIdAsc(portalId, projectId)
			.stream()
			.filter((list) -> includeArchived || !list.isArchived())
			.filter((list) -> !caller.client() || list.getVisibility() == TaskListVisibility.EXTERNAL)
			.map((list) -> this.mapper.toListResponse(list, counts.getOrDefault(list.getId(), new int[2])[0],
					counts.getOrDefault(list.getId(), new int[2])[1]))
			.toList();
	}

	private TaskListResponseDTO single(UUID portalId, TaskList list, TaskCaller caller) {
		int[] count = counts(portalId, list.getProjectId()).getOrDefault(list.getId(), new int[2]);
		return this.mapper.toListResponse(list, count[0], count[1]);
	}

	/** Per list: [tasks, closed tasks], trashed tasks excluded. */
	private Map<UUID, int[]> counts(UUID portalId, UUID projectId) {
		List<Task> tasks = this.taskRepository.findByPortalIdAndProjectIdAndDeletedAtIsNull(portalId, projectId);
		Map<UUID, LayoutStatusResponseDTO> statuses = this.statusResolver.byId(portalId,
				tasks.stream().map(Task::getLayoutId).toList());
		Map<UUID, int[]> counts = new HashMap<>();
		for (Task task : tasks) {
			int[] count = counts.computeIfAbsent(task.getTaskListId(), (id) -> new int[2]);
			count[0]++;
			LayoutStatusResponseDTO status = statuses.get(task.getStatusId());
			if (status != null && TaskConstants.STATUS_TYPE_CLOSED.equals(status.statusType())) {
				count[1]++;
			}
		}
		return counts;
	}

	private static String requireName(String raw, List<TaskList> existing, UUID selfId) {
		String name = raw == null ? "" : raw.trim();
		if (name.isEmpty()) {
			throw invalid(TaskConstants.DETAIL_NAME, TaskConstants.MESSAGE_NAME_REQUIRED);
		}
		if (name.length() > TaskConstants.LIST_NAME_MAX_LENGTH) {
			throw invalid(TaskConstants.DETAIL_NAME, TaskConstants.MESSAGE_NAME_TOO_LONG);
		}
		if (existing.stream().anyMatch((list) -> !list.getId().equals(selfId) && list.getName().equalsIgnoreCase(name))) {
			throw invalid(TaskConstants.DETAIL_NAME, TaskConstants.MESSAGE_NAME_TAKEN);
		}
		return name;
	}

	private static AppException invalid(String property, String message) {
		Map<String, Object> details = new HashMap<>();
		details.put(property, message);
		return new AppException(HttpStatus.BAD_REQUEST, AppConstants.ERROR_VALIDATION_FAILED,
				AppConstants.MESSAGE_VALIDATION_FAILED, details);
	}

	private static AppException notFound() {
		return new AppException(HttpStatus.NOT_FOUND, TaskConstants.ERROR_TASK_LIST_NOT_FOUND,
				TaskConstants.MESSAGE_TASK_LIST_NOT_FOUND);
	}

}

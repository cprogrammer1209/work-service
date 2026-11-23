package com.projecty.work.modules.task.internal.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.projecty.work.modules.customfield.api.CustomFieldLookUpService;
import com.projecty.work.modules.customfield.api.dto.CustomFieldResponseDTO;
import com.projecty.work.modules.customfield.api.dto.LayoutStatusResponseDTO;
import com.projecty.work.modules.project.api.dto.ProjectSummaryDTO;
import com.projecty.work.modules.shared.constants.PermissionConstants;
import com.projecty.work.modules.task.internal.constants.TaskConstants;
import com.projecty.work.modules.task.internal.dto.TaskResponseDTO;
import com.projecty.work.modules.task.internal.entity.Task;
import com.projecty.work.modules.task.internal.entity.TaskList;
import com.projecty.work.modules.task.internal.mapper.TaskMapper;
import com.projecty.work.modules.task.internal.repository.TaskCommentRepository;
import com.projecty.work.modules.task.internal.repository.TaskListRepository;

import org.springframework.stereotype.Component;

/**
 * Turns tasks of one project into responses. Everything the views share (lists, statuses, subtask and comment counts,
 * the active custom fields) is loaded once into a {@link Context}.
 */
@Component
public class TaskViewBuilder {

	private final TaskListRepository listRepository;

	private final TaskCommentRepository commentRepository;

	private final TaskStatusResolver statusResolver;

	private final CustomFieldLookUpService customFieldLookUpService;

	private final TaskMapper mapper;

	public TaskViewBuilder(TaskListRepository listRepository, TaskCommentRepository commentRepository,
			TaskStatusResolver statusResolver, CustomFieldLookUpService customFieldLookUpService, TaskMapper mapper) {
		this.listRepository = listRepository;
		this.commentRepository = commentRepository;
		this.statusResolver = statusResolver;
		this.customFieldLookUpService = customFieldLookUpService;
		this.mapper = mapper;
	}

	public record Context(ProjectSummaryDTO project, TaskCaller caller, Map<UUID, TaskList> lists,
			Map<UUID, LayoutStatusResponseDTO> statuses, Set<String> activeKeys, Map<UUID, List<Task>> children,
			Map<UUID, Integer> comments) {
	}

	/**
	 * @param tasks every task of the project that a view or a count may need (the live ones, plus trashed ones that
	 * are shown); trashed ones are left out of the subtask counts
	 */
	public Context context(UUID portalId, ProjectSummaryDTO project, TaskCaller caller, Collection<Task> tasks) {
		Map<UUID, TaskList> lists = this.listRepository
			.findByPortalIdAndProjectIdOrderBySortOrderAscCreatedAtAscIdAsc(portalId, project.id())
			.stream()
			.collect(Collectors.toMap(TaskList::getId, (list) -> list));
		Map<UUID, LayoutStatusResponseDTO> statuses = this.statusResolver.byId(portalId,
				tasks.stream().map(Task::getLayoutId).toList());
		Set<String> activeKeys = this.customFieldLookUpService.findFields(portalId, TaskConstants.CUSTOM_FIELD_ENTITY)
			.stream()
			.map(CustomFieldResponseDTO::apiName)
			.collect(Collectors.toSet());
		Map<UUID, List<Task>> children = new HashMap<>();
		for (Task task : tasks) {
			if (task.getParentTaskId() != null && !task.isTrashed()) {
				children.computeIfAbsent(task.getParentTaskId(), (id) -> new ArrayList<>()).add(task);
			}
		}
		Map<UUID, Integer> comments = new HashMap<>();
		this.commentRepository.countByTask(portalId, project.id())
			.forEach((count) -> comments.put(count.getTaskId(), (int) count.getTotal()));
		return new Context(project, caller, lists, statuses, activeKeys, children, comments);
	}

	/** The answers of active fields only; keys of inactive or deleted fields are never shown. */
	public TaskResponseDTO view(Context context, Task task) {
		return view(context, task, activeValues(task, context.activeKeys()));
	}

	public TaskResponseDTO view(Context context, Task task, Map<String, Object> customValues) {
		List<Task> children = context.children().getOrDefault(task.getId(), List.of());
		int completed = (int) children.stream().filter((child) -> isClosed(context, child)).count();
		return this.mapper.toResponse(task, context.project().key(), context.project().name(),
				context.lists().get(task.getTaskListId()),
				context.statuses().get(task.getStatusId()), customValues, children.size(), completed,
				context.comments().getOrDefault(task.getId(), 0), context.caller().can(PermissionConstants.TASKS_EDIT),
				context.caller().can(PermissionConstants.TASKS_DELETE));
	}

	public boolean isClosed(Context context, Task task) {
		LayoutStatusResponseDTO status = context.statuses().get(task.getStatusId());
		return status != null && TaskConstants.STATUS_TYPE_CLOSED.equals(status.statusType());
	}

	public static Map<String, Object> activeValues(Task task, Collection<String> keys) {
		Map<String, Object> values = new HashMap<>();
		task.getCustomValues().forEach((key, value) -> {
			if (keys.contains(key)) {
				values.put(key, value);
			}
		});
		return values;
	}

}

package com.projecty.work.modules.task.internal.helper;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.projecty.work.modules.outbox.api.OutboxWriteService;
import com.projecty.work.modules.outbox.api.dto.OutboxEventCreateDTO;
import com.projecty.work.modules.project.api.ProjectCacheService;
import com.projecty.work.modules.shared.constants.OutboxEventConstants;
import com.projecty.work.modules.task.internal.constants.TaskConstants;
import com.projecty.work.modules.task.internal.entity.Task;
import com.projecty.work.modules.task.internal.entity.TaskActivity;
import com.projecty.work.modules.task.internal.entity.TaskList;
import com.projecty.work.modules.task.internal.mapper.TaskMapper;
import com.projecty.work.modules.task.internal.repository.TaskActivityRepository;

import org.springframework.stereotype.Component;

/**
 * Writes what happened to a task: its history entry and its outbox event. Both belong to the same transaction as the
 * change; the relay sends the event to Kafka afterwards (never for a rolled-back change).
 */
@Component
public class TaskHistory {

	private final TaskActivityRepository activityRepository;

	private final OutboxWriteService outboxWriteService;

	private final ProjectCacheService projectCache;

	private final Clock clock;

	public TaskHistory(TaskActivityRepository activityRepository, OutboxWriteService outboxWriteService,
			ProjectCacheService projectCache, Clock clock) {
		this.activityRepository = activityRepository;
		this.outboxWriteService = outboxWriteService;
		this.projectCache = projectCache;
		this.clock = clock;
	}

	public void record(Task task, String action, String field, String oldValue, String newValue, String detail,
			UUID actorId, String actorName) {
		this.activityRepository.save(new TaskActivity(task.getPortalId(), task.getId(), action, field,
				abbreviate(oldValue, TaskConstants.ACTIVITY_VALUE_MAX_LENGTH),
				abbreviate(newValue, TaskConstants.ACTIVITY_VALUE_MAX_LENGTH),
				abbreviate(detail, TaskConstants.ACTIVITY_DETAIL_MAX_LENGTH), actorId, actorName,
				this.clock.instant()));
	}

	/** Ids and before/after ids only; names and texts stay out of events. */
	public void publish(String eventType, Task task, String projectKey, UUID actorId, String actorName,
			Map<String, Object> extra) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("portalId", task.getPortalId());
		payload.put("taskId", task.getId());
		payload.put("projectId", task.getProjectId());
		payload.put("key", TaskMapper.displayKey(projectKey, task.getTaskNumber()));
		payload.putAll(extra);
		this.outboxWriteService.publish(new OutboxEventCreateDTO(task.getPortalId(),
				OutboxEventConstants.AGGREGATE_TASK, task.getId(), eventType, actorId, actorName, payload));
		// Every task and comment write publishes, so this drops what was cached about the project's tasks.
		this.projectCache.tasksChanged(task.getPortalId(), task.getProjectId());
	}

	public void publishList(String eventType, TaskList list, UUID actorId, String actorName, String change) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("portalId", list.getPortalId());
		payload.put("taskListId", list.getId());
		payload.put("projectId", list.getProjectId());
		payload.put("change", change);
		this.outboxWriteService.publish(new OutboxEventCreateDTO(list.getPortalId(),
				OutboxEventConstants.AGGREGATE_TASK_LIST, list.getId(), eventType, actorId, actorName, payload));
		this.projectCache.tasksChanged(list.getPortalId(), list.getProjectId());
	}

	/** For list changes that publish no event (the "General" list created or brought back on first use). */
	public void listsChanged(UUID portalId, UUID projectId) {
		this.projectCache.tasksChanged(portalId, projectId);
	}

	private static String abbreviate(String text, int max) {
		if (text == null || text.length() <= max) {
			return text;
		}
		return text.substring(0, max - 1) + "…";
	}

}

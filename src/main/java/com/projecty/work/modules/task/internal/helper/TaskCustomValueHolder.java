package com.projecty.work.modules.task.internal.helper;

import java.util.UUID;

import com.projecty.work.modules.customfield.api.CustomValueHolder;
import com.projecty.work.modules.task.internal.constants.TaskConstants;
import com.projecty.work.modules.task.internal.repository.TaskRepository;

import org.springframework.stereotype.Component;

/**
 * Lets the customfield module count and clear the answers stored on tasks, and count the tasks of a layout, without
 * it touching the task table.
 */
@Component
public class TaskCustomValueHolder implements CustomValueHolder {

	private final TaskRepository taskRepository;

	public TaskCustomValueHolder(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	@Override
	public String entityType() {
		return TaskConstants.CUSTOM_FIELD_ENTITY;
	}

	@Override
	public long countUsing(UUID portalId, String apiName) {
		return this.taskRepository.countWithCustomKey(portalId, apiName);
	}

	@Override
	public long countUsingLayout(UUID portalId, UUID layoutId) {
		return this.taskRepository.countByPortalIdAndLayoutId(portalId, layoutId);
	}

	@Override
	public void removeKey(UUID portalId, String apiName) {
		this.taskRepository.removeCustomKey(portalId, apiName);
	}

}

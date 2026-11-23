package com.projecty.work.modules.task.internal.helper;

import java.util.UUID;

import com.projecty.work.modules.customfield.api.StatusUsageHolder;
import com.projecty.work.modules.task.internal.repository.TaskRepository;

import org.springframework.stereotype.Component;

/** Tells the customfield module how many tasks are in a status, so a status in use cannot be deleted. */
@Component
public class TaskStatusUsageHolder implements StatusUsageHolder {

	private final TaskRepository taskRepository;

	public TaskStatusUsageHolder(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	@Override
	public long countUsingStatus(UUID portalId, UUID statusId) {
		return this.taskRepository.countByPortalIdAndStatusId(portalId, statusId);
	}

}

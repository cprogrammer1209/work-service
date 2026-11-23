package com.projecty.work.modules.task.internal.service;

import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.task.internal.dto.TaskAcrossFilterDTO;
import com.projecty.work.modules.task.internal.dto.TaskCreateDTO;
import com.projecty.work.modules.task.internal.dto.TaskDetailResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskFilterDTO;
import com.projecty.work.modules.task.internal.dto.TaskPageResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskUpdateDTO;

/**
 * The tasks of the projects of the request's portal (X-Portal-Id). Every method takes the caller; the project rules
 * decide which projects they see, and a Client sees only tasks of External lists.
 */
public interface TaskService {

	/** A project's tasks as a flat list (subtasks included), in list order then number. */
	List<TaskResponseDTO> list(UUID actorId, UUID projectId, TaskFilterDTO filter);

	/**
	 * The live tasks of every project the caller may see, by due date (none last), then project key and number. A
	 * Client sees only tasks of External lists. Paged with an offset cursor.
	 */
	TaskPageResponseDTO listAcrossProjects(UUID actorId, TaskAcrossFilterDTO filter);

	TaskResponseDTO create(UUID actorId, String actorName, UUID projectId, TaskCreateDTO request);

	TaskDetailResponseDTO get(UUID actorId, UUID taskId);

	TaskResponseDTO update(UUID actorId, String actorName, UUID taskId, TaskUpdateDTO request);

	TaskResponseDTO changeStatus(UUID actorId, String actorName, UUID taskId, UUID statusId);

	/** Moves the task to another task layout; its status is matched by name, else it gets the layout's default. */
	TaskDetailResponseDTO changeLayout(UUID actorId, String actorName, UUID taskId, UUID layoutId);

	/** Trashes the task and the subtasks that are not trashed already. */
	void trash(UUID actorId, String actorName, UUID taskId);

	/** Restores the task and the subtasks that were trashed with it. */
	TaskResponseDTO restore(UUID actorId, String actorName, UUID taskId);

	TaskResponseDTO addFollower(UUID actorId, String actorName, UUID taskId, UUID userId);

	TaskResponseDTO removeFollower(UUID actorId, String actorName, UUID taskId, UUID userId);

}

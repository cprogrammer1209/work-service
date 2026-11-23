package com.projecty.work.modules.task.internal.service;

import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.task.internal.dto.TaskListCreateDTO;
import com.projecty.work.modules.task.internal.dto.TaskListResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskListUpdateDTO;
import com.projecty.work.modules.task.internal.dto.TaskOrderDTO;
import com.projecty.work.modules.task.internal.entity.TaskList;

/**
 * The task lists of the projects of the request's portal (X-Portal-Id). Every method takes the caller; what they may
 * see depends on the project rules and, for a Client, on the list being External.
 */
public interface TaskListService {

	/** The project's lists in order; a project with none gets its default list "General" first. */
	List<TaskListResponseDTO> list(UUID actorId, UUID projectId, boolean includeArchived);

	TaskListResponseDTO create(UUID actorId, String actorName, UUID projectId, TaskListCreateDTO request);

	TaskListResponseDTO update(UUID actorId, String actorName, UUID listId, TaskListUpdateDTO request);

	TaskListResponseDTO archive(UUID actorId, String actorName, UUID listId, boolean archived);

	/** Lists named first take that order; the rest follow in their current order. Answers with every list. */
	List<TaskListResponseDTO> reorder(UUID actorId, String actorName, UUID projectId, TaskOrderDTO request);

	/**
	 * The list a new task goes into when none is chosen: the first active list. A project with no active list gets
	 * "General" (created, or unarchived when it exists).
	 */
	TaskList defaultList(UUID portalId, UUID projectId, UUID userId);

}

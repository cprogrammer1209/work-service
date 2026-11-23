package com.projecty.work.modules.task.internal.service;

import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.task.internal.dto.TaskCommentBodyDTO;
import com.projecty.work.modules.task.internal.dto.TaskCommentResponseDTO;

/**
 * Comments on tasks. Anyone who can see a task can comment; people change or delete their own comments, and Admins
 * can delete any.
 */
public interface TaskCommentService {

	List<TaskCommentResponseDTO> list(UUID actorId, UUID taskId);

	TaskCommentResponseDTO add(UUID actorId, String actorName, UUID taskId, TaskCommentBodyDTO request);

	TaskCommentResponseDTO edit(UUID actorId, String actorName, UUID commentId, TaskCommentBodyDTO request);

	void delete(UUID actorId, String actorName, UUID commentId);

}

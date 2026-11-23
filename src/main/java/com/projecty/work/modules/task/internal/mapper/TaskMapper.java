package com.projecty.work.modules.task.internal.mapper;

import java.util.Map;

import com.projecty.work.modules.customfield.api.dto.LayoutStatusResponseDTO;
import com.projecty.work.modules.task.internal.constants.TaskConstants;
import com.projecty.work.modules.task.internal.dto.TaskActivityResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskCommentResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskListResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskResponseDTO;
import com.projecty.work.modules.task.internal.entity.Task;
import com.projecty.work.modules.task.internal.entity.TaskActivity;
import com.projecty.work.modules.task.internal.entity.TaskComment;
import com.projecty.work.modules.task.internal.entity.TaskList;

import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

	/** WEB and 42 give WEB-T42. */
	public static String displayKey(String projectKey, int number) {
		return projectKey + TaskConstants.KEY_INFIX + number;
	}

	public TaskResponseDTO toResponse(Task task, String projectKey, String projectName, TaskList list, LayoutStatusResponseDTO status,
			Map<String, Object> customValues, int subtaskCount, int completedSubtaskCount, int commentCount,
			boolean canEdit, boolean canDelete) {
		return new TaskResponseDTO(task.getId(), task.getProjectId(), projectKey,
				projectName,
				displayKey(projectKey, task.getTaskNumber()), task.getTaskNumber(), task.getTaskListId(),
				list == null ? null : list.getName(), task.getParentTaskId(), task.getDepth(), task.getTitle(),
				task.getDescription(), task.getStatusId(), status == null ? null : status.name(),
				status == null ? null : status.statusType(), status == null ? null : status.color(),
				task.getPriority(), task.getStartDate(), task.getDueDate(), task.getWorkHours(), task.getCompletion(),
				task.getOwnerIds(), task.getFollowerIds(), task.getTags(), customValues, task.getLayoutId(),
				subtaskCount, completedSubtaskCount, commentCount, task.getClosedAt(), task.getCreatedAt(),
				task.getCreatedBy(), task.getUpdatedAt(), task.getDeletedAt(), canEdit, canDelete);
	}

	public TaskListResponseDTO toListResponse(TaskList list, int taskCount, int completedCount) {
		return new TaskListResponseDTO(list.getId(), list.getProjectId(), list.getName(), list.getVisibility(),
				list.getSortOrder(), list.isArchived(), taskCount, completedCount, list.getCreatedAt());
	}

	public TaskActivityResponseDTO toActivity(TaskActivity activity) {
		return new TaskActivityResponseDTO(activity.getId(), activity.getAction(), activity.getField(),
				activity.getOldValue(), activity.getNewValue(), activity.getDetail(), activity.getActorUserId(),
				activity.getActorName(), activity.getCreatedAt());
	}

	public TaskCommentResponseDTO toComment(TaskComment comment, boolean canEdit, boolean canDelete) {
		return new TaskCommentResponseDTO(comment.getId(), comment.getTaskId(), comment.getAuthorUserId(),
				comment.getAuthorName(), comment.getBody(), comment.isEdited(), comment.getCreatedAt(),
				comment.getUpdatedAt(), canEdit, canDelete);
	}

}

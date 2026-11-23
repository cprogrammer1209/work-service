package com.projecty.work.modules.task.internal.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.projecty.work.modules.customfield.api.dto.LayoutResponseDTO;
import com.projecty.work.modules.customfield.api.dto.LayoutStatusResponseDTO;

/**
 * A task with everything its page shows: the layout it uses (placed fields), the answers of active fields the layout
 * does not show ({@code hiddenValues}, kept), the statuses to choose from, all its subtasks at any depth (flat, by
 * depth then number; {@code parentId} builds the tree), its history (newest first, at most 100 entries), its comments
 * (oldest first) and the ids of its followers.
 */
public record TaskDetailResponseDTO(TaskResponseDTO task, LayoutResponseDTO layout, Map<String, Object> hiddenValues,
		List<LayoutStatusResponseDTO> statuses, List<TaskResponseDTO> subtasks,
		List<TaskActivityResponseDTO> activity, List<TaskCommentResponseDTO> comments, List<UUID> followers) {
}

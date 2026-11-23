package com.projecty.work.modules.task.internal.controller;

import java.util.List;
import java.util.UUID;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.dto.AppResponse;
import com.projecty.work.common.security.annotation.RequirePermission;
import com.projecty.work.modules.shared.constants.PermissionConstants;
import com.projecty.work.modules.task.internal.dto.TaskCommentBodyDTO;
import com.projecty.work.modules.task.internal.dto.TaskCommentResponseDTO;
import com.projecty.work.modules.task.internal.service.TaskCommentService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Comments on tasks: anyone who can see the task (TASKS_VIEW) can read and write them. */
@RestController
@RequestMapping("/api/work")
public class TaskCommentController {

	private final TaskCommentService commentService;

	public TaskCommentController(TaskCommentService commentService) {
		this.commentService = commentService;
	}

	@GetMapping("/tasks/{taskId}/comments")
	@RequirePermission(PermissionConstants.TASKS_VIEW)
	public AppResponse<List<TaskCommentResponseDTO>> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId) {
		return AppResponse.ok(this.commentService.list(actor(jwt), taskId));
	}

	@PostMapping("/tasks/{taskId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	@RequirePermission(PermissionConstants.TASKS_VIEW)
	public AppResponse<TaskCommentResponseDTO> add(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId,
			@RequestBody TaskCommentBodyDTO request) {
		return AppResponse.ok(this.commentService.add(actor(jwt), actorName(jwt), taskId, request));
	}

	@PatchMapping("/task-comments/{commentId}")
	@RequirePermission(PermissionConstants.TASKS_VIEW)
	public AppResponse<TaskCommentResponseDTO> edit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID commentId,
			@RequestBody TaskCommentBodyDTO request) {
		return AppResponse.ok(this.commentService.edit(actor(jwt), actorName(jwt), commentId, request));
	}

	@DeleteMapping("/task-comments/{commentId}")
	@RequirePermission(PermissionConstants.TASKS_VIEW)
	public AppResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID commentId) {
		this.commentService.delete(actor(jwt), actorName(jwt), commentId);
		return AppResponse.ok();
	}

	private static UUID actor(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	private static String actorName(Jwt jwt) {
		return jwt.getClaimAsString(AppConstants.CLAIM_NAME);
	}

}

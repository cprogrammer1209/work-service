package com.projecty.work.modules.task.internal.controller;

import java.util.List;
import java.util.UUID;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.dto.AppResponse;
import com.projecty.work.common.security.annotation.RequirePermission;
import com.projecty.work.modules.shared.constants.PermissionConstants;
import com.projecty.work.modules.task.internal.dto.TaskListCreateDTO;
import com.projecty.work.modules.task.internal.dto.TaskListResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskListUpdateDTO;
import com.projecty.work.modules.task.internal.dto.TaskOrderDTO;
import com.projecty.work.modules.task.internal.service.TaskListService;

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

/** Task lists of the request's portal (X-Portal-Id). */
@RestController
@RequestMapping("/api/work")
public class TaskListController {

	private final TaskListService listService;

	public TaskListController(TaskListService listService) {
		this.listService = listService;
	}

	@GetMapping("/projects/{projectId}/task-lists")
	@RequirePermission(PermissionConstants.TASKS_VIEW)
	public AppResponse<List<TaskListResponseDTO>> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
			@RequestParam(defaultValue = "false") boolean includeArchived) {
		return AppResponse.ok(this.listService.list(actor(jwt), projectId, includeArchived));
	}

	@PostMapping("/projects/{projectId}/task-lists")
	@ResponseStatus(HttpStatus.CREATED)
	@RequirePermission(PermissionConstants.TASKS_CREATE)
	public AppResponse<TaskListResponseDTO> create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
			@RequestBody TaskListCreateDTO request) {
		return AppResponse.ok(this.listService.create(actor(jwt), actorName(jwt), projectId, request));
	}

	@PutMapping("/projects/{projectId}/task-lists/order")
	@RequirePermission(PermissionConstants.TASKS_EDIT)
	public AppResponse<List<TaskListResponseDTO>> reorder(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID projectId, @Valid @RequestBody TaskOrderDTO request) {
		return AppResponse.ok(this.listService.reorder(actor(jwt), actorName(jwt), projectId, request));
	}

	@PatchMapping("/task-lists/{listId}")
	@RequirePermission(PermissionConstants.TASKS_EDIT)
	public AppResponse<TaskListResponseDTO> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listId,
			@RequestBody TaskListUpdateDTO request) {
		return AppResponse.ok(this.listService.update(actor(jwt), actorName(jwt), listId, request));
	}

	@PostMapping("/task-lists/{listId}/archive")
	@RequirePermission(PermissionConstants.TASKS_EDIT)
	public AppResponse<TaskListResponseDTO> archive(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listId) {
		return AppResponse.ok(this.listService.archive(actor(jwt), actorName(jwt), listId, true));
	}

	@PostMapping("/task-lists/{listId}/unarchive")
	@RequirePermission(PermissionConstants.TASKS_EDIT)
	public AppResponse<TaskListResponseDTO> unarchive(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listId) {
		return AppResponse.ok(this.listService.archive(actor(jwt), actorName(jwt), listId, false));
	}

	private static UUID actor(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	private static String actorName(Jwt jwt) {
		return jwt.getClaimAsString(AppConstants.CLAIM_NAME);
	}

}

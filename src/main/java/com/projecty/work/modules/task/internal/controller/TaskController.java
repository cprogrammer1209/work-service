package com.projecty.work.modules.task.internal.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.dto.AppResponse;
import com.projecty.work.common.security.annotation.RequirePermission;
import com.projecty.work.modules.shared.constants.PermissionConstants;
import com.projecty.work.modules.task.internal.dto.TaskAcrossFilterDTO;
import com.projecty.work.modules.task.internal.dto.TaskCreateDTO;
import com.projecty.work.modules.task.internal.dto.TaskDetailResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskFilterDTO;
import com.projecty.work.modules.task.internal.dto.TaskLayoutUpdateDTO;
import com.projecty.work.modules.task.internal.dto.TaskPageResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskStatusUpdateDTO;
import com.projecty.work.modules.task.internal.dto.TaskUpdateDTO;
import com.projecty.work.modules.task.internal.entity.TaskPriority;
import com.projecty.work.modules.task.internal.service.TaskService;

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

/** Tasks of the request's portal (X-Portal-Id). The service applies project visibility and the Client rule. */
@RestController
@RequestMapping("/api/work")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@GetMapping("/projects/{projectId}/tasks")
	@RequirePermission(PermissionConstants.TASKS_VIEW)
	public AppResponse<List<TaskResponseDTO>> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
			@RequestParam(required = false) UUID listId, @RequestParam(required = false) UUID status,
			@RequestParam(required = false) String statusType, @RequestParam(required = false) UUID ownerId,
			@RequestParam(required = false) TaskPriority priority, @RequestParam(required = false) String q,
			@RequestParam(required = false) UUID parentId, @RequestParam(defaultValue = "false") boolean trashed) {
		return AppResponse.ok(this.taskService.list(actor(jwt), projectId,
				new TaskFilterDTO(listId, status, statusType, ownerId, priority, q, parentId, trashed)));
	}

	/** The tasks of every project the caller may see (the Tasks module); scope=mine keeps those they own. */
	@GetMapping("/tasks")
	@RequirePermission(PermissionConstants.TASKS_VIEW)
	public AppResponse<TaskPageResponseDTO> listAcrossProjects(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String scope, @RequestParam(required = false) UUID projectId,
			@RequestParam(required = false) String statusType, @RequestParam(required = false) UUID ownerId,
			@RequestParam(required = false) TaskPriority priority, @RequestParam(required = false) String q,
			@RequestParam(required = false) LocalDate dueBefore, @RequestParam(required = false) LocalDate dueAfter,
			@RequestParam(required = false) Integer limit, @RequestParam(required = false) String cursor) {
		return AppResponse.ok(this.taskService.listAcrossProjects(actor(jwt), new TaskAcrossFilterDTO(scope,
				projectId, statusType, ownerId, priority, q, dueBefore, dueAfter, limit, cursor)));
	}

	@PostMapping("/projects/{projectId}/tasks")
	@ResponseStatus(HttpStatus.CREATED)
	@RequirePermission(PermissionConstants.TASKS_CREATE)
	public AppResponse<TaskResponseDTO> create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
			@RequestBody TaskCreateDTO request) {
		return AppResponse.ok(this.taskService.create(actor(jwt), actorName(jwt), projectId, request));
	}

	@GetMapping("/tasks/{taskId}")
	@RequirePermission(PermissionConstants.TASKS_VIEW)
	public AppResponse<TaskDetailResponseDTO> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId) {
		return AppResponse.ok(this.taskService.get(actor(jwt), taskId));
	}

	@PatchMapping("/tasks/{taskId}")
	@RequirePermission(PermissionConstants.TASKS_EDIT)
	public AppResponse<TaskResponseDTO> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId,
			@RequestBody TaskUpdateDTO request) {
		return AppResponse.ok(this.taskService.update(actor(jwt), actorName(jwt), taskId, request));
	}

	@PostMapping("/tasks/{taskId}/status")
	@RequirePermission(PermissionConstants.TASKS_EDIT)
	public AppResponse<TaskResponseDTO> changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId,
			@Valid @RequestBody TaskStatusUpdateDTO request) {
		return AppResponse.ok(this.taskService.changeStatus(actor(jwt), actorName(jwt), taskId, request.statusId()));
	}

	@PutMapping("/tasks/{taskId}/layout")
	@RequirePermission(PermissionConstants.TASKS_EDIT)
	public AppResponse<TaskDetailResponseDTO> changeLayout(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID taskId, @Valid @RequestBody TaskLayoutUpdateDTO request) {
		return AppResponse.ok(this.taskService.changeLayout(actor(jwt), actorName(jwt), taskId, request.layoutId()));
	}

	@DeleteMapping("/tasks/{taskId}")
	@RequirePermission(PermissionConstants.TASKS_DELETE)
	public AppResponse<Void> trash(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId) {
		this.taskService.trash(actor(jwt), actorName(jwt), taskId);
		return AppResponse.ok();
	}

	@PostMapping("/tasks/{taskId}/restore")
	@RequirePermission(PermissionConstants.TASKS_DELETE)
	public AppResponse<TaskResponseDTO> restore(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId) {
		return AppResponse.ok(this.taskService.restore(actor(jwt), actorName(jwt), taskId));
	}

	/** Following yourself needs only TASKS_VIEW; adding someone else needs TASKS_EDIT (the service checks). */
	@PostMapping("/tasks/{taskId}/followers/{userId}")
	@RequirePermission(PermissionConstants.TASKS_VIEW)
	public AppResponse<TaskResponseDTO> addFollower(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId,
			@PathVariable UUID userId) {
		return AppResponse.ok(this.taskService.addFollower(actor(jwt), actorName(jwt), taskId, userId));
	}

	@DeleteMapping("/tasks/{taskId}/followers/{userId}")
	@RequirePermission(PermissionConstants.TASKS_VIEW)
	public AppResponse<TaskResponseDTO> removeFollower(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId,
			@PathVariable UUID userId) {
		return AppResponse.ok(this.taskService.removeFollower(actor(jwt), actorName(jwt), taskId, userId));
	}

	private static UUID actor(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	private static String actorName(Jwt jwt) {
		return jwt.getClaimAsString(AppConstants.CLAIM_NAME);
	}

}

package com.projecty.work.modules.project.internal.controller;

import java.util.List;
import java.util.UUID;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.dto.AppResponse;
import com.projecty.work.common.security.annotation.RequirePermission;
import com.projecty.work.modules.shared.constants.PermissionConstants;
import com.projecty.work.modules.project.internal.dto.KeyAvailabilityResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectCreateDTO;
import com.projecty.work.modules.project.internal.dto.ProjectDetailResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectIdResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectLayoutUpdateDTO;
import com.projecty.work.modules.project.internal.dto.ProjectMemberAddDTO;
import com.projecty.work.modules.project.internal.dto.ProjectMemberResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectStatusResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectStatusUpdateDTO;
import com.projecty.work.modules.project.internal.dto.ProjectUpdateDTO;
import com.projecty.work.modules.project.internal.service.ProjectService;

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

/**
 * Projects of the request's portal (X-Portal-Id). The permission on each endpoint is the base right; the service
 * then applies the scope (Admin, owner, project Manager) and the visibility rules.
 */
@RestController
@RequestMapping("/api/work")
public class ProjectController {

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping("/projects")
	@RequirePermission(PermissionConstants.PROJECTS_VIEW)
	public AppResponse<List<ProjectResponseDTO>> list(@AuthenticationPrincipal Jwt jwt) {
		return AppResponse.ok(this.projectService.list(actor(jwt)));
	}

	@PostMapping("/projects")
	@ResponseStatus(HttpStatus.CREATED)
	@RequirePermission(PermissionConstants.PROJECTS_CREATE)
	public AppResponse<ProjectIdResponseDTO> create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody ProjectCreateDTO request) {
		return AppResponse.ok(this.projectService.create(actor(jwt), actorName(jwt), request));
	}

	@GetMapping("/projects/trash")
	@RequirePermission(PermissionConstants.PROJECTS_DELETE)
	public AppResponse<List<ProjectResponseDTO>> trash(@AuthenticationPrincipal Jwt jwt) {
		return AppResponse.ok(this.projectService.listTrash(actor(jwt)));
	}

	@GetMapping("/projects/key-availability")
	@RequirePermission(PermissionConstants.PROJECTS_CREATE)
	public AppResponse<KeyAvailabilityResponseDTO> keyAvailability(@RequestParam String key) {
		return AppResponse.ok(this.projectService.checkKey(key));
	}

	@GetMapping("/projects/{projectId}")
	@RequirePermission(PermissionConstants.PROJECTS_VIEW)
	public AppResponse<ProjectDetailResponseDTO> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
		return AppResponse.ok(this.projectService.get(actor(jwt), projectId));
	}

	@PatchMapping("/projects/{projectId}")
	@RequirePermission(PermissionConstants.PROJECTS_EDIT)
	public AppResponse<ProjectResponseDTO> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
			@RequestBody ProjectUpdateDTO request) {
		return AppResponse.ok(this.projectService.update(actor(jwt), actorName(jwt), projectId, request));
	}

	@PostMapping("/projects/{projectId}/status")
	@RequirePermission(PermissionConstants.PROJECTS_EDIT)
	public AppResponse<ProjectResponseDTO> changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
			@Valid @RequestBody ProjectStatusUpdateDTO request) {
		return AppResponse.ok(this.projectService.changeStatus(actor(jwt), actorName(jwt), projectId, request.status()));
	}

	/** The project's own settings: which layout it uses. */
	@PutMapping("/projects/{projectId}/layout")
	@RequirePermission(PermissionConstants.PROJECTS_EDIT)
	public AppResponse<ProjectDetailResponseDTO> changeLayout(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID projectId, @Valid @RequestBody ProjectLayoutUpdateDTO request) {
		return AppResponse.ok(this.projectService.changeLayout(actor(jwt), actorName(jwt), projectId, request.layoutId()));
	}

	/** Which layout the project's tasks use. */
	@PutMapping("/projects/{projectId}/task-layout")
	@RequirePermission(PermissionConstants.PROJECTS_EDIT)
	public AppResponse<ProjectDetailResponseDTO> changeTaskLayout(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID projectId, @Valid @RequestBody ProjectLayoutUpdateDTO request) {
		return AppResponse.ok(this.projectService.changeTaskLayout(actor(jwt), actorName(jwt), projectId, request.layoutId()));
	}

	@DeleteMapping("/projects/{projectId}")
	@RequirePermission(PermissionConstants.PROJECTS_DELETE)
	public AppResponse<Void> trash(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
		this.projectService.trash(actor(jwt), actorName(jwt), projectId);
		return AppResponse.ok();
	}

	@PostMapping("/projects/{projectId}/restore")
	@RequirePermission(PermissionConstants.PROJECTS_DELETE)
	public AppResponse<ProjectResponseDTO> restore(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
		return AppResponse.ok(this.projectService.restore(actor(jwt), actorName(jwt), projectId));
	}

	@PostMapping("/projects/{projectId}/members")
	@RequirePermission(PermissionConstants.PROJECTS_EDIT)
	public AppResponse<List<ProjectMemberResponseDTO>> addMember(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID projectId, @Valid @RequestBody ProjectMemberAddDTO request) {
		return AppResponse.ok(this.projectService.addMember(actor(jwt), actorName(jwt), projectId, request));
	}

	@DeleteMapping("/projects/{projectId}/members/{userId}")
	@RequirePermission(PermissionConstants.PROJECTS_EDIT)
	public AppResponse<List<ProjectMemberResponseDTO>> removeMember(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID projectId, @PathVariable UUID userId) {
		return AppResponse.ok(this.projectService.removeMember(actor(jwt), actorName(jwt), projectId, userId));
	}

	@GetMapping("/project-statuses")
	@RequirePermission(PermissionConstants.PROJECTS_VIEW)
	public AppResponse<List<ProjectStatusResponseDTO>> statuses() {
		return AppResponse.ok(this.projectService.statuses());
	}

	private static UUID actor(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	private static String actorName(Jwt jwt) {
		return jwt.getClaimAsString(AppConstants.CLAIM_NAME);
	}

}

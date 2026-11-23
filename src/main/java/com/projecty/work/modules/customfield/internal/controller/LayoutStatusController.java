package com.projecty.work.modules.customfield.internal.controller;

import java.util.List;
import java.util.UUID;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.dto.AppResponse;
import com.projecty.work.common.security.annotation.RequirePermission;
import com.projecty.work.modules.customfield.api.dto.LayoutStatusResponseDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutStatusCreateDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutStatusOrderDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutStatusUpdateDTO;
import com.projecty.work.modules.customfield.internal.service.LayoutStatusService;
import com.projecty.work.modules.shared.constants.PermissionConstants;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The statuses of a TASK layout. Everyone who may use the module can read them; only Admins (CUSTOM_FIELDS_MANAGE)
 * change them. Every change answers with the layout's full status list.
 */
@RestController
@RequestMapping("/api/work/layouts/{layoutId}/statuses")
public class LayoutStatusController {

	private final LayoutStatusService statusService;

	public LayoutStatusController(LayoutStatusService statusService) {
		this.statusService = statusService;
	}

	@GetMapping
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_VIEW)
	public AppResponse<List<LayoutStatusResponseDTO>> list(@PathVariable UUID layoutId) {
		return AppResponse.ok(this.statusService.list(layoutId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<List<LayoutStatusResponseDTO>> create(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID layoutId, @Valid @RequestBody LayoutStatusCreateDTO request) {
		return AppResponse.ok(this.statusService.create(actor(jwt), actorName(jwt), layoutId, request));
	}

	@PatchMapping("/{statusId}")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<List<LayoutStatusResponseDTO>> update(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID layoutId, @PathVariable UUID statusId, @Valid @RequestBody LayoutStatusUpdateDTO request) {
		return AppResponse.ok(this.statusService.update(actor(jwt), actorName(jwt), layoutId, statusId, request));
	}

	@DeleteMapping("/{statusId}")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<List<LayoutStatusResponseDTO>> delete(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID layoutId, @PathVariable UUID statusId) {
		return AppResponse.ok(this.statusService.delete(actor(jwt), actorName(jwt), layoutId, statusId));
	}

	@PutMapping("/order")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<List<LayoutStatusResponseDTO>> reorder(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID layoutId, @Valid @RequestBody LayoutStatusOrderDTO request) {
		return AppResponse.ok(this.statusService.reorder(actor(jwt), actorName(jwt), layoutId, request));
	}

	private static UUID actor(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	private static String actorName(Jwt jwt) {
		return jwt.getClaimAsString(AppConstants.CLAIM_NAME);
	}

}

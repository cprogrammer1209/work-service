package com.projecty.work.modules.customfield.internal.controller;

import java.util.List;
import java.util.UUID;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.dto.AppResponse;
import com.projecty.work.common.security.annotation.RequirePermission;
import com.projecty.work.modules.customfield.api.dto.LayoutResponseDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutCreateDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutEditorResponseDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutSummaryResponseDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutUpdateDTO;
import com.projecty.work.modules.customfield.internal.service.LayoutService;
import com.projecty.work.modules.shared.constants.PermissionConstants;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Layouts of the request's portal (X-Portal-Id). Everyone who may use the module can read them to draw a form;
 * only Admins (CUSTOM_FIELDS_MANAGE) change them. Layouts are deactivated, never deleted.
 */
@RestController
@RequestMapping("/api/work/layouts")
public class LayoutController {

	private final LayoutService layoutService;

	public LayoutController(LayoutService layoutService) {
		this.layoutService = layoutService;
	}

	/** Every layout of a module with its counts: the layouts list. */
	@GetMapping
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_VIEW)
	public AppResponse<List<LayoutSummaryResponseDTO>> list(@RequestParam String entityType) {
		return AppResponse.ok(this.layoutService.listSummaries(entityType));
	}

	/** The active layouts ready to draw a form (default first): what a "choose a layout" picker offers. */
	@GetMapping("/active")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_VIEW)
	public AppResponse<List<LayoutResponseDTO>> active(@RequestParam String entityType) {
		return AppResponse.ok(this.layoutService.listActive(entityType));
	}

	/** One layout as stored plus every field that could be placed in it: the layout builder. */
	@GetMapping("/{layoutId}")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<LayoutEditorResponseDTO> editor(@PathVariable UUID layoutId) {
		return AppResponse.ok(this.layoutService.editor(layoutId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<LayoutSummaryResponseDTO> create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody LayoutCreateDTO request) {
		return AppResponse.ok(this.layoutService.create(actor(jwt), actorName(jwt), request));
	}

	/** Saves the whole layout: name, description, sections and placed fields. */
	@PutMapping("/{layoutId}")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<LayoutEditorResponseDTO> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID layoutId,
			@Valid @RequestBody LayoutUpdateDTO request) {
		return AppResponse.ok(this.layoutService.update(actor(jwt), actorName(jwt), layoutId, request));
	}

	@PostMapping("/{layoutId}/default")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<LayoutSummaryResponseDTO> makeDefault(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID layoutId) {
		return AppResponse.ok(this.layoutService.makeDefault(actor(jwt), actorName(jwt), layoutId));
	}

	@PostMapping("/{layoutId}/activate")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<LayoutSummaryResponseDTO> activate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID layoutId) {
		return AppResponse.ok(this.layoutService.activate(actor(jwt), actorName(jwt), layoutId));
	}

	@PostMapping("/{layoutId}/deactivate")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<LayoutSummaryResponseDTO> deactivate(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID layoutId) {
		return AppResponse.ok(this.layoutService.deactivate(actor(jwt), actorName(jwt), layoutId));
	}

	private static UUID actor(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	private static String actorName(Jwt jwt) {
		return jwt.getClaimAsString(AppConstants.CLAIM_NAME);
	}

}

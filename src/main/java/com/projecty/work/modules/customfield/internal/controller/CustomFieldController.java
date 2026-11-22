package com.projecty.work.modules.customfield.internal.controller;

import java.util.List;
import java.util.UUID;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.dto.AppResponse;
import com.projecty.work.common.security.annotation.RequirePermission;
import com.projecty.work.modules.shared.constants.PermissionConstants;
import com.projecty.work.modules.customfield.api.dto.CustomFieldResponseDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldCreateDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldOptionCreateDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldOptionUpdateDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldReorderDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldUpdateDTO;
import com.projecty.work.modules.customfield.internal.dto.CustomFieldUsageResponseDTO;
import com.projecty.work.modules.customfield.internal.service.CustomFieldService;

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
 * Custom fields of the request's portal (X-Portal-Id). Everyone who may use the module can read the active fields
 * to draw a form; only Admins (CUSTOM_FIELDS_MANAGE) change them.
 */
@RestController
@RequestMapping("/api/work/custom-fields")
public class CustomFieldController {

	private final CustomFieldService customFieldService;

	public CustomFieldController(CustomFieldService customFieldService) {
		this.customFieldService = customFieldService;
	}

	/** The active fields of a module, in form order. */
	@GetMapping
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_VIEW)
	public AppResponse<List<CustomFieldResponseDTO>> list(@RequestParam String entityType) {
		return AppResponse.ok(this.customFieldService.listActive(entityType));
	}

	/** Active and inactive fields, for the admin screen. */
	@GetMapping("/all")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<List<CustomFieldResponseDTO>> listAll(@RequestParam String entityType) {
		return AppResponse.ok(this.customFieldService.listAll(entityType));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<CustomFieldResponseDTO> create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CustomFieldCreateDTO request) {
		return AppResponse.ok(this.customFieldService.create(actor(jwt), actorName(jwt), request));
	}

	@PutMapping("/reorder")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<List<CustomFieldResponseDTO>> reorder(@Valid @RequestBody CustomFieldReorderDTO request) {
		return AppResponse.ok(this.customFieldService.reorder(request));
	}

	@PatchMapping("/{fieldId}")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<CustomFieldResponseDTO> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID fieldId,
			@Valid @RequestBody CustomFieldUpdateDTO request) {
		return AppResponse.ok(this.customFieldService.update(actor(jwt), actorName(jwt), fieldId, request));
	}

	@PostMapping("/{fieldId}/deactivate")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<CustomFieldResponseDTO> deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID fieldId) {
		return AppResponse.ok(this.customFieldService.deactivate(actor(jwt), actorName(jwt), fieldId));
	}

	@PostMapping("/{fieldId}/activate")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<CustomFieldResponseDTO> activate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID fieldId) {
		return AppResponse.ok(this.customFieldService.activate(actor(jwt), actorName(jwt), fieldId));
	}

	/** How many records have an answer for the field: the impact of deleting it. */
	@GetMapping("/{fieldId}/usage")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<CustomFieldUsageResponseDTO> usage(@PathVariable UUID fieldId) {
		return AppResponse.ok(this.customFieldService.usage(fieldId));
	}

	@DeleteMapping("/{fieldId}")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID fieldId) {
		this.customFieldService.delete(actor(jwt), actorName(jwt), fieldId);
		return AppResponse.ok();
	}

	@PostMapping("/{fieldId}/options")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<CustomFieldResponseDTO> addOption(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID fieldId,
			@Valid @RequestBody CustomFieldOptionCreateDTO request) {
		return AppResponse.ok(this.customFieldService.addOption(actor(jwt), actorName(jwt), fieldId, request));
	}

	/** Choices are deactivated, never deleted: old records keep showing them (CUS-BR-03). */
	@PatchMapping("/{fieldId}/options/{optionId}")
	@RequirePermission(PermissionConstants.CUSTOM_FIELDS_MANAGE)
	public AppResponse<CustomFieldResponseDTO> updateOption(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID fieldId, @PathVariable UUID optionId,
			@Valid @RequestBody CustomFieldOptionUpdateDTO request) {
		return AppResponse.ok(this.customFieldService.updateOption(actor(jwt), actorName(jwt), fieldId, optionId, request));
	}

	private static UUID actor(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	private static String actorName(Jwt jwt) {
		return jwt.getClaimAsString(AppConstants.CLAIM_NAME);
	}

}

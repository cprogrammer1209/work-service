package com.projecty.work.modules.customfield.internal.dto;

/** A placed field exactly as stored (not resolved): what the layout builder edits. */
public record LayoutPlacementResponseDTO(String apiName, Boolean mandatory, boolean readOnly, Object defaultValue,
		Boolean wide) {
}

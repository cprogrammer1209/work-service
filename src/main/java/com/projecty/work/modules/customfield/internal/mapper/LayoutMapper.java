package com.projecty.work.modules.customfield.internal.mapper;

import java.util.List;

import com.projecty.work.modules.customfield.api.dto.CustomFieldOptionResponseDTO;
import com.projecty.work.modules.customfield.api.dto.LayoutFieldResponseDTO;
import com.projecty.work.modules.customfield.api.dto.LayoutStatusResponseDTO;
import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;
import com.projecty.work.modules.customfield.internal.dto.LayoutPlacementResponseDTO;
import com.projecty.work.modules.customfield.internal.entity.CustomField;
import com.projecty.work.modules.customfield.internal.entity.CustomFieldOption;
import com.projecty.work.modules.customfield.internal.entity.FieldType;
import com.projecty.work.modules.customfield.internal.entity.LayoutField;
import com.projecty.work.modules.customfield.internal.entity.LayoutStatus;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class LayoutMapper {

	private final CustomFieldMapper fieldMapper;

	private final ObjectMapper objectMapper;

	public LayoutMapper(CustomFieldMapper fieldMapper, ObjectMapper objectMapper) {
		this.fieldMapper = fieldMapper;
		this.objectMapper = objectMapper;
	}

	/** A placed field with the layout's rules applied (a layout override beats the field). */
	public LayoutFieldResponseDTO toFieldResponse(CustomField field, LayoutField placement,
			List<CustomFieldOption> options) {
		boolean mandatory = placement.getMandatory() != null ? placement.getMandatory() : field.isMandatory();
		boolean wide = placement.getWide() != null ? placement.getWide()
				: CustomFieldConstants.WIDE_TYPES.contains(field.getFieldType());
		List<CustomFieldOptionResponseDTO> choices = options.stream().map(this.fieldMapper::toOption).toList();
		return new LayoutFieldResponseDTO(field.getId(), field.getApiName(), field.getLabel(), field.getDescription(),
				field.getFieldType().name(), mandatory, field.isMandatory(), placement.isReadOnly(), wide,
				field.isUniqueField(), field.isSensitive(), effectiveDefault(field, placement, options),
				field.getConfig(), choices);
	}

	public LayoutStatusResponseDTO toStatus(LayoutStatus status) {
		return new LayoutStatusResponseDTO(status.getId(), status.getName(), status.getStatusType().name(),
				status.getColor(), status.getSortOrder(), status.isDefaultStatus());
	}

	public LayoutPlacementResponseDTO toPlacement(CustomField field, LayoutField placement) {
		return new LayoutPlacementResponseDTO(field.getApiName(), placement.getMandatory(), placement.isReadOnly(),
				parse(placement.getDefaultValue()), placement.getWide());
	}

	/** The layout's own default, else the field's: the default choice(s) for pickers, the stored default otherwise. */
	private Object effectiveDefault(CustomField field, LayoutField placement, List<CustomFieldOption> options) {
		if (placement.getDefaultValue() != null) {
			return parse(placement.getDefaultValue());
		}
		FieldType type = field.getFieldType();
		if (type == FieldType.PICKLIST) {
			return options.stream()
				.filter((option) -> option.isActive() && option.isDefaultOption())
				.map((option) -> option.getId().toString())
				.findFirst()
				.orElse(null);
		}
		if (type == FieldType.MULTI_PICKLIST) {
			List<String> ids = options.stream()
				.filter((option) -> option.isActive() && option.isDefaultOption())
				.map((option) -> option.getId().toString())
				.toList();
			return ids.isEmpty() ? null : ids;
		}
		return parse(field.getDefaultValue());
	}

	private Object parse(String json) {
		return json == null ? null : this.objectMapper.readValue(json, Object.class);
	}

}

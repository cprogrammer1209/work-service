package com.projecty.work.modules.customfield.internal.mapper;

import java.util.List;

import com.projecty.work.modules.customfield.api.dto.CustomFieldOptionResponseDTO;
import com.projecty.work.modules.customfield.api.dto.CustomFieldResponseDTO;
import com.projecty.work.modules.customfield.internal.entity.CustomField;
import com.projecty.work.modules.customfield.internal.entity.CustomFieldOption;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class CustomFieldMapper {

	private final ObjectMapper objectMapper;

	public CustomFieldMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public CustomFieldResponseDTO toResponse(CustomField field, List<CustomFieldOption> options) {
		return new CustomFieldResponseDTO(field.getId(), field.getEntityType(), field.getApiName(), field.getLabel(),
				field.getDescription(), field.getFieldType().name(), field.isMandatory(), field.isUniqueField(),
				field.isSensitive(), parseDefault(field.getDefaultValue()), field.getConfig(), field.getStatus().name(),
				field.getSortOrder(), options.stream().map(this::toOption).toList());
	}

	public CustomFieldOptionResponseDTO toOption(CustomFieldOption option) {
		return new CustomFieldOptionResponseDTO(option.getId(), option.getLabel(), option.getColor(),
				option.getSortOrder(), option.isDefaultOption(), option.isActive());
	}

	private Object parseDefault(String json) {
		return json == null ? null : this.objectMapper.readValue(json, Object.class);
	}

}

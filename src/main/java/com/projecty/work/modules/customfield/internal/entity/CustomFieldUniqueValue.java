package com.projecty.work.modules.customfield.internal.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A value reserved by one record for a field marked unique. The primary key (field, value) is what refuses a
 * duplicate.
 */
@Entity
@Table(name = "custom_field_unique_value")
@IdClass(CustomFieldUniqueValue.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomFieldUniqueValue {

	@Id
	private UUID fieldId;

	@Id
	private String valueKey;

	private UUID portalId;

	private UUID entityId;

	public CustomFieldUniqueValue(UUID portalId, UUID fieldId, String valueKey, UUID entityId) {
		this.portalId = portalId;
		this.fieldId = fieldId;
		this.valueKey = valueKey;
		this.entityId = entityId;
	}

	public record Key(UUID fieldId, String valueKey) implements Serializable {
	}

}

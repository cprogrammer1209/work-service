package com.projecty.work.modules.customfield.internal.entity;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * The definition of one custom field ("the question"). The answers live in the owning module's own table. The key
 * ({@code apiName}) and the type never change; a deleted field keeps its row so its key is never reused.
 */
@Entity
@Table(name = "custom_field")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomField {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	private UUID portalId;

	private String entityType;

	private String apiName;

	private String label;

	private String description;

	@Enumerated(EnumType.STRING)
	private FieldType fieldType;

	private boolean mandatory;

	@Column(name = "is_unique")
	private boolean uniqueField;

	private boolean sensitive;

	/** JSON text of the default answer (null when none); picklists take defaults from their options instead. */
	@JdbcTypeCode(SqlTypes.JSON)
	private String defaultValue;

	@JdbcTypeCode(SqlTypes.JSON)
	private Map<String, Object> config = new HashMap<>();

	@Enumerated(EnumType.STRING)
	private FieldStatus status = FieldStatus.ACTIVE;

	private int sortOrder;

	private Instant createdAt;

	private UUID createdBy;

	private Instant updatedAt;

	private UUID updatedBy;

	public CustomField(UUID portalId, String entityType, String apiName, String label, String description,
			FieldType fieldType, boolean mandatory, boolean uniqueField, boolean sensitive, String defaultValue,
			Map<String, Object> config, int sortOrder, UUID userId, Instant now) {
		this.portalId = portalId;
		this.entityType = entityType;
		this.apiName = apiName;
		this.label = label;
		this.description = description;
		this.fieldType = fieldType;
		this.mandatory = mandatory;
		this.uniqueField = uniqueField;
		this.sensitive = sensitive;
		this.defaultValue = defaultValue;
		this.config = new HashMap<>(config);
		this.sortOrder = sortOrder;
		this.createdAt = now;
		this.createdBy = userId;
		this.updatedAt = now;
		this.updatedBy = userId;
	}

	public void edit(String label, String description, boolean mandatory, boolean sensitive, String defaultValue,
			Map<String, Object> config, UUID userId, Instant now) {
		this.label = label;
		this.description = description;
		this.mandatory = mandatory;
		this.sensitive = sensitive;
		this.defaultValue = defaultValue;
		this.config = new HashMap<>(config);
		touch(userId, now);
	}

	public void changeStatus(FieldStatus status, UUID userId, Instant now) {
		this.status = status;
		touch(userId, now);
	}

	public void moveTo(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	private void touch(UUID userId, Instant now) {
		this.updatedAt = now;
		this.updatedBy = userId;
	}

}

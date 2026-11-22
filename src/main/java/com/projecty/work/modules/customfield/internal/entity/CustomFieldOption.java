package com.projecty.work.modules.customfield.internal.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * One choice of a picklist or multi-select. Records store the option's id, so renaming never breaks them. An
 * inactive option is hidden from new choices but stays on old records (CUS-BR-03).
 */
@Entity
@Table(name = "custom_field_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomFieldOption {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	private UUID portalId;

	private UUID fieldId;

	private String label;

	private String color;

	private int sortOrder;

	@Column(name = "is_default")
	private boolean defaultOption;

	private boolean active = true;

	public CustomFieldOption(UUID portalId, UUID fieldId, String label, String color, int sortOrder,
			boolean defaultOption) {
		this.portalId = portalId;
		this.fieldId = fieldId;
		this.label = label;
		this.color = color;
		this.sortOrder = sortOrder;
		this.defaultOption = defaultOption;
	}

	public void edit(String label, String color, boolean defaultOption, boolean active) {
		this.label = label;
		this.color = color;
		this.defaultOption = defaultOption;
		this.active = active;
	}

	public void clearDefault() {
		this.defaultOption = false;
	}

}

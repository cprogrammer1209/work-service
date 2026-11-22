package com.projecty.work.modules.customfield.internal.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * A custom field placed in a section of a layout, with the layout's own rules for it. A null {@code mandatory} or
 * {@code wide} means "as the field says"; a layout override beats the field (CUS-BR-04).
 */
@Entity
@Table(name = "layout_field")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LayoutField {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	private UUID portalId;

	private UUID layoutId;

	private UUID sectionId;

	private UUID fieldId;

	private int sortOrder;

	private Boolean mandatory;

	private boolean readOnly;

	/** JSON text of the layout's own default answer (null = use the field's default). */
	@JdbcTypeCode(SqlTypes.JSON)
	private String defaultValue;

	private Boolean wide;

	public LayoutField(UUID portalId, UUID layoutId, UUID sectionId, UUID fieldId, int sortOrder, Boolean mandatory,
			boolean readOnly, String defaultValue, Boolean wide) {
		this.portalId = portalId;
		this.layoutId = layoutId;
		this.sectionId = sectionId;
		this.fieldId = fieldId;
		this.sortOrder = sortOrder;
		this.mandatory = mandatory;
		this.readOnly = readOnly;
		this.defaultValue = defaultValue;
		this.wide = wide;
	}

}

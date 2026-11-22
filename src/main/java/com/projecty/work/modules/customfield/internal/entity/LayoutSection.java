package com.projecty.work.modules.customfield.internal.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/** A titled group of fields inside a layout. */
@Entity
@Table(name = "layout_section")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LayoutSection {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	private UUID portalId;

	private UUID layoutId;

	private String name;

	private boolean collapsible;

	private int sortOrder;

	public LayoutSection(UUID portalId, UUID layoutId, String name, boolean collapsible, int sortOrder) {
		this.portalId = portalId;
		this.layoutId = layoutId;
		this.name = name;
		this.collapsible = collapsible;
		this.sortOrder = sortOrder;
	}

}

package com.projecty.work.modules.customfield.internal.entity;

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
import org.hibernate.annotations.UuidGenerator;

/**
 * A status of a TASK layout. Tasks store the id, so renaming a status changes nothing on them. Exactly one status
 * of a layout is the default, and it is an OPEN one.
 */
@Entity
@Table(name = "layout_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LayoutStatus {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	private UUID portalId;

	private UUID layoutId;

	private String name;

	@Enumerated(EnumType.STRING)
	private LayoutStatusType statusType;

	private String color;

	private int sortOrder;

	@Column(name = "is_default")
	private boolean defaultStatus;

	public LayoutStatus(UUID portalId, UUID layoutId, String name, LayoutStatusType statusType, String color,
			int sortOrder, boolean defaultStatus) {
		this.portalId = portalId;
		this.layoutId = layoutId;
		this.name = name;
		this.statusType = statusType;
		this.color = color;
		this.sortOrder = sortOrder;
		this.defaultStatus = defaultStatus;
	}

	public void edit(String name, LayoutStatusType statusType, String color) {
		this.name = name;
		this.statusType = statusType;
		this.color = color;
	}

	public void changeSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public void changeDefault(boolean defaultStatus) {
		this.defaultStatus = defaultStatus;
	}

}

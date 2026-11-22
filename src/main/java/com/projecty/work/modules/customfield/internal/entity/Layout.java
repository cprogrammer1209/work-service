package com.projecty.work.modules.customfield.internal.entity;

import java.time.Instant;
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
 * A layout of a module: which custom fields a record shows, in which sections. Its sections and placed fields are in
 * {@link LayoutSection} and {@link LayoutField}. Layouts are deactivated, never deleted.
 */
@Entity
@Table(name = "layout")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Layout {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	private UUID portalId;

	private String entityType;

	private String name;

	private String description;

	@Column(name = "is_default")
	private boolean defaultLayout;

	private boolean active = true;

	private Instant createdAt;

	private UUID createdBy;

	private Instant updatedAt;

	private UUID updatedBy;

	public Layout(UUID portalId, String entityType, String name, String description, boolean defaultLayout,
			UUID userId, Instant now) {
		this.portalId = portalId;
		this.entityType = entityType;
		this.name = name;
		this.description = description;
		this.defaultLayout = defaultLayout;
		this.createdAt = now;
		this.createdBy = userId;
		this.updatedAt = now;
		this.updatedBy = userId;
	}

	public void edit(String name, String description, UUID userId, Instant now) {
		this.name = name;
		this.description = description;
		touch(userId, now);
	}

	public void changeDefault(boolean defaultLayout, UUID userId, Instant now) {
		this.defaultLayout = defaultLayout;
		touch(userId, now);
	}

	public void changeActive(boolean active, UUID userId, Instant now) {
		this.active = active;
		touch(userId, now);
	}

	private void touch(UUID userId, Instant now) {
		this.updatedAt = now;
		this.updatedBy = userId;
	}

}

package com.projecty.work.modules.project.internal.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
 * The container of all work in a portal. The key never changes after creation, because item ids like WEB-T42
 * depend on it. A trashed project keeps its row (and its key) until it is restored.
 */
@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	private UUID portalId;

	private String projectKey;

	private String name;

	private String description;

	private UUID ownerUserId;

	private String status;

	private LocalDate startDate;

	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	private ProjectVisibility visibility;

	/** The answers to the portal's custom fields, keyed by each field's api_name (see the customfield module). */
	@JdbcTypeCode(SqlTypes.JSON)
	private Map<String, Object> customValues = new HashMap<>();

	/** The layout (which custom fields, in which sections) this project uses; see the customfield module. */
	private UUID layoutId;

	private Instant createdAt;

	private Instant updatedAt;

	private Instant deletedAt;

	private UUID deletedByUserId;

	public Project(UUID portalId, String projectKey, String name, String description, UUID ownerUserId,
			String status, LocalDate startDate, LocalDate endDate, ProjectVisibility visibility, UUID layoutId,
			Instant now) {
		this.portalId = portalId;
		this.projectKey = projectKey;
		this.name = name;
		this.description = description;
		this.ownerUserId = ownerUserId;
		this.status = status;
		this.startDate = startDate;
		this.endDate = endDate;
		this.visibility = visibility;
		this.layoutId = layoutId;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public void edit(String name, String description, UUID ownerUserId, LocalDate startDate, LocalDate endDate,
			ProjectVisibility visibility, Instant now) {
		this.name = name;
		this.description = description;
		this.ownerUserId = ownerUserId;
		this.startDate = startDate;
		this.endDate = endDate;
		this.visibility = visibility;
		this.updatedAt = now;
	}

	public void changeCustomValues(Map<String, Object> customValues, Instant now) {
		this.customValues = new HashMap<>(customValues);
		this.updatedAt = now;
	}

	public void changeLayout(UUID layoutId, Instant now) {
		this.layoutId = layoutId;
		this.updatedAt = now;
	}

	public void changeStatus(String status, Instant now) {
		this.status = status;
		this.updatedAt = now;
	}

	public void trash(UUID userId, Instant now) {
		this.deletedAt = now;
		this.deletedByUserId = userId;
		this.updatedAt = now;
	}

	public void restore(Instant now) {
		this.deletedAt = null;
		this.deletedByUserId = null;
		this.updatedAt = now;
	}

	public boolean isTrashed() {
		return this.deletedAt != null;
	}

}

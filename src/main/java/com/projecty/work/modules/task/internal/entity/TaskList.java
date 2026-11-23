package com.projecty.work.modules.task.internal.entity;

import java.time.Instant;
import java.util.UUID;

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
 * A named group of tasks inside a project. Lists are archived, never deleted. A project's default list is called
 * "General" and is created the first time a project's tasks are used.
 */
@Entity
@Table(name = "task_list")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskList {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	private UUID portalId;

	private UUID projectId;

	private String name;

	@Enumerated(EnumType.STRING)
	private TaskListVisibility visibility;

	private int sortOrder;

	private boolean archived;

	private Instant createdAt;

	private UUID createdBy;

	private Instant updatedAt;

	public TaskList(UUID portalId, UUID projectId, String name, TaskListVisibility visibility, int sortOrder,
			UUID userId, Instant now) {
		this.portalId = portalId;
		this.projectId = projectId;
		this.name = name;
		this.visibility = visibility;
		this.sortOrder = sortOrder;
		this.createdBy = userId;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public void edit(String name, TaskListVisibility visibility, Instant now) {
		this.name = name;
		this.visibility = visibility;
		this.updatedAt = now;
	}

	public void changeArchived(boolean archived, Instant now) {
		this.archived = archived;
		this.updatedAt = now;
	}

	public void changeSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

}

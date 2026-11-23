package com.projecty.work.modules.task.internal.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * A unit of work in a project. The display id is the project key plus {@code -T} plus {@link #taskNumber}; the number
 * never changes and is never reused. A subtask lives in its parent's list. A trashed task keeps its row, its number
 * and its place until it is restored.
 */
@Entity
@Table(name = "task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	private UUID portalId;

	private UUID projectId;

	private UUID taskListId;

	private UUID parentTaskId;

	/** 1 for a top-level task, 2 for its subtasks, and so on. */
	private int depth = 1;

	private int taskNumber;

	private String title;

	private String description;

	/** The TASK layout (custom fields and statuses) this task uses. */
	private UUID layoutId;

	/** A status of that layout. */
	private UUID statusId;

	@Enumerated(EnumType.STRING)
	private TaskPriority priority = TaskPriority.NONE;

	private LocalDate startDate;

	private LocalDate dueDate;

	private BigDecimal workHours;

	private int completion;

	@JdbcTypeCode(SqlTypes.ARRAY)
	private UUID[] ownerIds = new UUID[0];

	@JdbcTypeCode(SqlTypes.ARRAY)
	private UUID[] followerIds = new UUID[0];

	@JdbcTypeCode(SqlTypes.ARRAY)
	private String[] tags = new String[0];

	/** The answers to the portal's TASK custom fields, keyed by api_name (see the customfield module). */
	@JdbcTypeCode(SqlTypes.JSON)
	private Map<String, Object> customValues = new HashMap<>();

	private Instant closedAt;

	private Instant createdAt;

	private UUID createdBy;

	private Instant updatedAt;

	private UUID updatedBy;

	private Instant deletedAt;

	private UUID deletedBy;

	public Task(UUID portalId, UUID projectId, UUID taskListId, UUID parentTaskId, int depth, int taskNumber,
			String title, UUID layoutId, UUID statusId, UUID userId, Instant now) {
		this.portalId = portalId;
		this.projectId = projectId;
		this.taskListId = taskListId;
		this.parentTaskId = parentTaskId;
		this.depth = depth;
		this.taskNumber = taskNumber;
		this.title = title;
		this.layoutId = layoutId;
		this.statusId = statusId;
		this.createdAt = now;
		this.createdBy = userId;
		this.updatedAt = now;
		this.updatedBy = userId;
	}

	public List<UUID> getOwnerIds() {
		return new ArrayList<>(List.of(this.ownerIds));
	}

	public List<UUID> getFollowerIds() {
		return new ArrayList<>(List.of(this.followerIds));
	}

	public List<String> getTags() {
		return new ArrayList<>(List.of(this.tags));
	}

	public void changeTitle(String title) {
		this.title = title;
	}

	public void changeDescription(String description) {
		this.description = description;
	}

	public void changePriority(TaskPriority priority) {
		this.priority = priority;
	}

	public void changeDates(LocalDate startDate, LocalDate dueDate) {
		this.startDate = startDate;
		this.dueDate = dueDate;
	}

	public void changeWorkHours(BigDecimal workHours) {
		this.workHours = workHours;
	}

	public void changeCompletion(int completion) {
		this.completion = completion;
	}

	public void changeOwners(List<UUID> ownerIds) {
		this.ownerIds = ownerIds.toArray(new UUID[0]);
	}

	public void changeFollowers(List<UUID> followerIds) {
		this.followerIds = followerIds.toArray(new UUID[0]);
	}

	public void changeTags(List<String> tags) {
		this.tags = tags.toArray(new String[0]);
	}

	public void changeCustomValues(Map<String, Object> customValues) {
		this.customValues = new HashMap<>(customValues);
	}

	public void changeList(UUID taskListId) {
		this.taskListId = taskListId;
	}

	public void changeParent(UUID parentTaskId, int depth) {
		this.parentTaskId = parentTaskId;
		this.depth = depth;
	}

	public void changeDepth(int depth) {
		this.depth = depth;
	}

	public void changeLayout(UUID layoutId) {
		this.layoutId = layoutId;
	}

	/** Moving into a Closed status sets {@code closedAt}; moving back to an Open one clears it. */
	public void changeStatus(UUID statusId, Instant closedAt) {
		this.statusId = statusId;
		this.closedAt = closedAt;
	}

	public void touch(UUID userId, Instant now) {
		this.updatedAt = now;
		this.updatedBy = userId;
	}

	public void trash(UUID userId, Instant now) {
		this.deletedAt = now;
		this.deletedBy = userId;
		this.updatedAt = now;
		this.updatedBy = userId;
	}

	public void restore(UUID userId, Instant now) {
		this.deletedAt = null;
		this.deletedBy = null;
		this.updatedAt = now;
		this.updatedBy = userId;
	}

	public boolean isTrashed() {
		return this.deletedAt != null;
	}

}

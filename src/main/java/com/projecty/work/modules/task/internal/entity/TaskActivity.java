package com.projecty.work.modules.task.internal.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * One thing that happened to a task. A field change has {@code field}, {@code oldValue} and {@code newValue} (ids stay
 * ids, the screen resolves names); other actions carry a short {@code detail}.
 */
@Entity
@Table(name = "task_activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskActivity {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	private UUID portalId;

	private UUID taskId;

	private String action;

	private String field;

	private String oldValue;

	private String newValue;

	private String detail;

	private UUID actorUserId;

	private String actorName;

	private Instant createdAt;

	public TaskActivity(UUID portalId, UUID taskId, String action, String field, String oldValue, String newValue,
			String detail, UUID actorUserId, String actorName, Instant now) {
		this.portalId = portalId;
		this.taskId = taskId;
		this.action = action;
		this.field = field;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.detail = detail;
		this.actorUserId = actorUserId;
		this.actorName = actorName;
		this.createdAt = now;
	}

}

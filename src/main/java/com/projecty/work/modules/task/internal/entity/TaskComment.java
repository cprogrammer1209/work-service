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

/** A comment on a task. The author's name is the one in the token when it was written. */
@Entity
@Table(name = "task_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskComment {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	private UUID portalId;

	private UUID taskId;

	private UUID authorUserId;

	private String authorName;

	private String body;

	private boolean edited;

	private Instant createdAt;

	private Instant updatedAt;

	public TaskComment(UUID portalId, UUID taskId, UUID authorUserId, String authorName, String body, Instant now) {
		this.portalId = portalId;
		this.taskId = taskId;
		this.authorUserId = authorUserId;
		this.authorName = authorName;
		this.body = body;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public void edit(String body, Instant now) {
		this.body = body;
		this.edited = true;
		this.updatedAt = now;
	}

}

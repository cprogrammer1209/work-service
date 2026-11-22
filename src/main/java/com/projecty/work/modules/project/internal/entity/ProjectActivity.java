package com.projecty.work.modules.project.internal.entity;

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
 * One thing that happened to a project, shown as its recent activity. The actor's name is the one in the token
 * when it happened.
 */
@Entity
@Table(name = "project_activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectActivity {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	private UUID portalId;

	private UUID projectId;

	private String action;

	private UUID actorUserId;

	private String actorName;

	private UUID targetUserId;

	private String detail;

	private Instant createdAt;

	public ProjectActivity(UUID portalId, UUID projectId, String action, UUID actorUserId, String actorName,
			UUID targetUserId, String detail, Instant now) {
		this.portalId = portalId;
		this.projectId = projectId;
		this.action = action;
		this.actorUserId = actorUserId;
		this.actorName = actorName;
		this.targetUserId = targetUserId;
		this.detail = detail;
		this.createdAt = now;
	}

}

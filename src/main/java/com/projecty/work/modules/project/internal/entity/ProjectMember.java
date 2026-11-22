package com.projecty.work.modules.project.internal.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A person in a project, with a role there. Only the user id is stored; names come from Platform.
 */
@Entity
@Table(name = "project_member")
@IdClass(ProjectMember.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {

	@Id
	private UUID projectId;

	@Id
	private UUID userId;

	private UUID portalId;

	@Enumerated(EnumType.STRING)
	private ProjectRole role;

	private Instant addedAt;

	public ProjectMember(UUID portalId, UUID projectId, UUID userId, ProjectRole role, Instant now) {
		this.portalId = portalId;
		this.projectId = projectId;
		this.userId = userId;
		this.role = role;
		this.addedAt = now;
	}

	public void changeRole(ProjectRole role) {
		this.role = role;
	}

	public record Key(UUID projectId, UUID userId) implements Serializable {
	}

}

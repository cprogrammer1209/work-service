package com.projecty.work.modules.access.internal.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Work's copy of one person's access in one portal, kept up to date from Platform's member events. It is never
 * written by a request, only by the event listener.
 */
@Entity
@Table(name = "member_access")
@IdClass(MemberAccess.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberAccess {

	@Id
	private UUID portalId;

	@Id
	private UUID userId;

	private String profileKey;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> permissions = List.of();

	private boolean active;

	private Instant updatedAt;

	public MemberAccess(UUID portalId, UUID userId, Instant now) {
		this.portalId = portalId;
		this.userId = userId;
		this.updatedAt = now;
	}

	/**
	 * The member (re)joined or came back. An event without a profile or permissions (older ones) keeps what is
	 * already known.
	 */
	public void activate(String profileKey, List<String> permissions, Instant now) {
		this.active = true;
		change(profileKey, permissions, now);
	}

	public void change(String profileKey, List<String> permissions, Instant now) {
		if (profileKey != null) {
			this.profileKey = profileKey;
		}
		if (permissions != null) {
			this.permissions = List.copyOf(permissions);
		}
		this.updatedAt = now;
	}

	public void deactivate(Instant now) {
		this.active = false;
		this.updatedAt = now;
	}

	public record Key(UUID portalId, UUID userId) implements Serializable {
	}

}

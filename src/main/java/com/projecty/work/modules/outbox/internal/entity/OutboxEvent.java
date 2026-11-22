package com.projecty.work.modules.outbox.internal.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * A business event waiting to be, or already, published. Only {@code publishedAt} ever changes.
 */
@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	private UUID portalId;

	private String aggregateType;

	private UUID aggregateId;

	private String eventType;

	@JdbcTypeCode(SqlTypes.JSON)
	private String payload;

	private short version;

	private UUID actorUserId;

	private String actorName;

	private Instant createdAt;

	private Instant publishedAt;

	public OutboxEvent(UUID portalId, String aggregateType, UUID aggregateId, String eventType, short version,
			UUID actorUserId, String actorName, String payload, Instant createdAt) {
		this.portalId = portalId;
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.version = version;
		this.actorUserId = actorUserId;
		this.actorName = actorName;
		this.payload = payload;
		this.createdAt = createdAt;
	}

	public void markPublished(Instant at) {
		this.publishedAt = at;
	}

}

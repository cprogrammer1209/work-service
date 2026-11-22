package com.projecty.work.modules.access.internal.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An event this service already applied. Delivery is at least once, so seeing an id here means "drop it".
 */
@Entity
@Table(name = "processed_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

	@Id
	private UUID eventId;

	private Instant processedAt;

	public ProcessedEvent(UUID eventId, Instant processedAt) {
		this.eventId = eventId;
		this.processedAt = processedAt;
	}

}
